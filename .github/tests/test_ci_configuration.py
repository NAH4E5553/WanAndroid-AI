"""Deterministic CI/OCR contract tests; no GitHub or model calls."""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import yaml

ROOT = Path(__file__).resolve().parents[2]


class UniqueKeyLoader(yaml.SafeLoader):
    pass


def unique_mapping(loader, node, deep=False):
    result = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=deep)
        if key in result:
            raise ValueError(f"duplicate YAML key: {key}")
        result[key] = loader.construct_object(value_node, deep=deep)
    return result


UniqueKeyLoader.add_constructor(
    yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, unique_mapping
)


def workflow(name):
    return yaml.load(
        (ROOT / ".github/workflows" / name).read_text(), Loader=UniqueKeyLoader
    )


class WorkflowContracts(unittest.TestCase):
    def test_exactly_one_android_and_one_ocr_workflow(self):
        self.assertEqual(
            {"android.yml", "open-code-review.yml"},
            {p.name for p in (ROOT / ".github/workflows").glob("*.yml")},
        )

    def test_android_events_do_not_skip_required_workflow(self):
        data = workflow("android.yml")
        self.assertEqual({"push", "pull_request", "workflow_dispatch"}, set(data["on"]))
        self.assertNotIn("paths", data["on"]["pull_request"])
        self.assertEqual(["main"], data["on"]["pull_request"]["branches"])
        self.assertEqual({"contents": "read"}, data["permissions"])

    def test_five_checks_use_new_project_tasks(self):
        data = workflow("android.yml")
        checks = data["jobs"]["checks"]
        self.assertEqual(
            {
                "Build": ":app:assembleDebug",
                "Unit Tests": "testAll",
                "Android Lint": "lintDebug",
                "Code Format": "spotlessCheck",
                "Architecture": "verifyArchitecture",
            },
            {row["name"]: row["task"] for row in checks["strategy"]["matrix"]["include"]},
        )
        self.assertEqual("always()", checks["if"])
        self.assertFalse(checks["strategy"]["fail-fast"])
        self.assertEqual("changes", checks["needs"])
        guard = checks["steps"][0]["run"]
        self.assertIn('"$CONFIG_RESULT" != "success"', guard)
        self.assertIn('"$RUN_ANDROID" != "true"', guard)
        self.assertIn('"$RUN_ANDROID" != "false"', guard)
        self.assertIn("exit 1", guard)
        self.assertNotIn("continue-on-error", checks)

    def test_unit_tests_upload_both_jvm_and_android_reports(self):
        rows = workflow("android.yml")["jobs"]["checks"]["strategy"]["matrix"]["include"]
        row = next(row for row in rows if row["name"] == "Unit Tests")
        reports = str(row)
        for path in ("**/build/reports/tests/test/", "**/build/test-results/test/",
                     "**/build/reports/tests/testDebugUnitTest/", "**/build/test-results/testDebugUnitTest/"):
            self.assertIn(path, reports)

    def test_configuration_checks_run_before_classification(self):
        steps = workflow("android.yml")["jobs"]["changes"]["steps"]
        tests = next(i for i, step in enumerate(steps) if "unittest" in step.get("run", ""))
        classifier = next(i for i, step in enumerate(steps) if step.get("id") == "classify")
        self.assertLess(tests, classifier)
        self.assertTrue(any("actionlint -shellcheck=" in step.get("run", "") for step in steps))
        command = steps[classifier]["run"]
        self.assertIn("workflow_dispatch", command)
        self.assertIn("^0+$", command)
        self.assertIn("set -euo pipefail", command)

    def test_ocr_disabled_by_default_and_no_fork_or_draft(self):
        data = workflow("open-code-review.yml")
        self.assertEqual({"pull_request"}, set(data["on"]))
        job = data["jobs"]["review"]
        self.assertIn("vars.OCR_ENABLED == 'true'", job["if"])
        self.assertIn("github.event.pull_request.draft == false", job["if"])
        self.assertIn("github.event.pull_request.head.repo.full_name == github.repository", job["if"])
        self.assertEqual({"contents": "read"}, data["permissions"])
        self.assertEqual({"contents": "read", "pull-requests": "write"}, job["permissions"])
        self.assertNotIn("continue-on-error", job)

    def test_ocr_pinned_configuration_and_secret_references(self):
        steps = workflow("open-code-review.yml")["jobs"]["review"]["steps"]
        action = steps[-1]
        self.assertEqual(
            "./build/ci-tools/ocr-action", action["uses"]
        )
        checkout = next(step for step in steps if step.get("uses", "").startswith("actions/checkout@"))
        self.assertEqual("${{ github.event.pull_request.base.sha }}", checkout["with"]["ref"])
        self.assertEqual(0, checkout["with"]["fetch-depth"])
        self.assertFalse(checkout["with"]["persist-credentials"])
        preparation = next(step for step in steps if "prepare_ocr_action.py" in step.get("run", ""))
        self.assertLess(steps.index(checkout), steps.index(preparation))
        self.assertNotIn("env", preparation)
        inputs = action["with"]
        self.assertEqual("1.11.1", inputs["ocr_version"])
        self.assertEqual("false", inputs["upload_artifacts"])
        self.assertEqual("true", inputs["checkpoint_range"])
        self.assertEqual("true", inputs["sticky_summary"])
        self.assertEqual("${{ secrets.OCR_LLM_URL }}", inputs["llm_url"])
        self.assertEqual("${{ secrets.OCR_LLM_AUTH_TOKEN }}", inputs["llm_auth_token"])
        self.assertEqual(".opencodereview/rule.json", inputs["rule"])
        self.assertIn('https://*', steps[0]["run"])
        self.assertIn('"$MODEL_PROTOCOL" != "false"', steps[0]["run"])
        self.assertNotIn("set -x", steps[0]["run"])

    def test_ocr_includes_dependency_schema_and_policy_changes(self):
        paths = workflow("open-code-review.yml")["on"]["pull_request"]["paths"]
        for required in (
            "**/*.kt", "**/*.kts", "**/*.xml", "gradle/**", "gradle.properties",
            "core/database/schemas/**", "AGENTS.md", ".github/**", ".opencodereview/**",
        ):
            self.assertIn(required, paths)
        self.assertIn("!.github/**/*.md", paths)

    def test_ocr_model_preflight_accepts_complete_configuration(self):
        result = self.run_model_preflight({})
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("", result.stdout)

    def test_ocr_model_preflight_rejects_missing_insecure_or_ambiguous_configuration(self):
        for override in (
            {"MODEL_TOKEN": ""}, {"MODEL_NAME": ""}, {"MODEL_URL": ""},
            {"MODEL_URL": "http://example.invalid"}, {"MODEL_PROTOCOL": ""},
            {"MODEL_PROTOCOL": "maybe"},
        ):
            with self.subTest(override=override):
                result = self.run_model_preflight(override)
                self.assertNotEqual(0, result.returncode)
                self.assertNotIn("synthetic-sensitive-value", result.stdout + result.stderr)

    @staticmethod
    def run_model_preflight(override):
        env = dict(os.environ)
        env.update(
            MODEL_URL="https://example.invalid", MODEL_TOKEN="synthetic-sensitive-value",
            MODEL_NAME="fixture", MODEL_PROTOCOL="false",
        )
        env.update(override)
        script = workflow("open-code-review.yml")["jobs"]["review"]["steps"][0]["run"]
        return subprocess.run(["bash", "-c", script], env=env, capture_output=True, text=True)

    def test_review_rules_match_new_project_not_mall_policy(self):
        data = json.loads((ROOT / ".opencodereview/rule.json").read_text())
        self.assertGreaterEqual(len(data["rules"]), 6)
        for rule in data["rules"]:
            self.assertEqual({"path", "rule", "merge_system_rule"}, set(rule))
            self.assertTrue(rule["merge_system_rule"])
        combined = " ".join(rule["rule"] for rule in data["rules"])
        self.assertNotIn("支付", combined)
        self.assertNotIn("android.enableJetifier=true", combined)
        self.assertIn("不禁止按需使用 MMKV", combined)
        for concept in ("Cookie", "分类", "收藏", "Room", "WebView", "取消"):
            self.assertIn(concept, combined)

    def test_yaml_duplicate_keys_are_rejected(self):
        with self.assertRaises(ValueError):
            yaml.load("jobs: {}\njobs: {}\n", Loader=UniqueKeyLoader)

    def test_corrupt_tool_download_never_installs(self):
        spec = importlib.util.spec_from_file_location(
            "installer", ROOT / ".github/scripts/install_actionlint.py"
        )
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        scratch = ROOT / "build/ci-tests"
        scratch.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=scratch) as directory:
            target = Path(directory) / "actionlint"
            with self.assertRaises(ValueError):
                module.install_verified(b"bad archive", "0" * 64, target)
            self.assertFalse(target.exists())


if __name__ == "__main__":
    unittest.main()
