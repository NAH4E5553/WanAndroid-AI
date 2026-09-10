"""Git fixtures live ONLY in this new project's ignored build/ci-tests directory."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / ".github/scripts/should-run-android-ci.sh"


class ChangeClassifierTests(unittest.TestCase):
    def setUp(self):
        scratch = ROOT / "build/ci-tests"
        scratch.mkdir(parents=True, exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(prefix="classifier-", dir=scratch)
        self.addCleanup(self.temp.cleanup)
        self.repo = Path(self.temp.name)
        self.env = {k: v for k, v in os.environ.items() if not k.startswith("GIT_")}
        self.env.update(
            GIT_CONFIG_NOSYSTEM="1",
            GIT_CONFIG_GLOBAL=os.devnull,
            GIT_AUTHOR_NAME="CI Fixture",
            GIT_AUTHOR_EMAIL="ci@example.invalid",
            GIT_COMMITTER_NAME="CI Fixture",
            GIT_COMMITTER_EMAIL="ci@example.invalid",
        )
        self.git("init", "-b", "main")
        self.write("README.md", "fixture\n")
        self.base = self.commit()

    def git(self, *args):
        return subprocess.run(
            ["git", "-c", "commit.gpgsign=false", "-c", f"core.hooksPath={self.repo / 'no-hooks'}", *args],
            cwd=self.repo, env=self.env, check=True, capture_output=True, text=True,
        ).stdout.strip()

    def write(self, path, text="synthetic fixture\n"):
        target = self.repo / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text)

    def commit(self):
        self.git("add", "--all")
        self.git("commit", "-m", "test fixture")
        return self.git("rev-parse", "HEAD")

    def classify(self, base=None, head=None, event="push"):
        return subprocess.run(
            ["bash", str(SCRIPT), base or self.base, head or self.git("rev-parse", "HEAD"), event],
            cwd=self.repo, env=self.env, capture_output=True, text=True,
        )

    def expect_change(self, path, expected):
        self.write(path)
        self.commit()
        result = self.classify()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(expected, result.stdout.strip())

    def test_known_documentation_skips_android(self):
        self.expect_change("docs/guide.md", "false")

    def test_kotlin_runs_android(self):
        self.expect_change("feature/home/Example.kt", "true")

    def test_unknown_file_is_conservative(self):
        self.expect_change("new-config.unknown", "true")

    def test_agents_inside_docs_is_not_skip_safe(self):
        self.expect_change("docs/AGENTS.md", "true")

    def test_version_catalog_runs_android(self):
        self.expect_change("gradle/libs.versions.toml", "true")

    def test_ocr_rule_changes_are_validated_and_run_android(self):
        self.expect_change(".opencodereview/rule.json", "true")

    def test_ci_workflow_runs_android(self):
        self.expect_change(".github/workflows/android.yml", "true")

    def test_spaces_and_newlines_in_paths_are_not_split(self):
        self.expect_change("core/space and\nnewline.kt", "true")

    def test_code_renamed_into_docs_still_runs(self):
        self.write("Example.kt", "same content\n")
        self.base = self.commit()
        (self.repo / "docs").mkdir()
        self.git("mv", "Example.kt", "docs/example.md")
        self.commit()
        result = self.classify()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("true", result.stdout.strip())

    def test_deleted_code_runs(self):
        self.write("Example.kt")
        self.base = self.commit()
        (self.repo / "Example.kt").unlink()
        self.commit()
        result = self.classify()
        self.assertEqual("true", result.stdout.strip())

    def test_empty_diff_is_error(self):
        self.assertNotEqual(0, self.classify().returncode)

    def test_nonexistent_commit_is_error(self):
        self.assertNotEqual(0, self.classify(base="f" * 40).returncode)

    def test_ref_option_injection_is_rejected(self):
        self.assertNotEqual(0, self.classify(base="--help").returncode)

    def test_unsupported_event_is_error(self):
        self.assertNotEqual(0, self.classify(event="unknown").returncode)

    def test_pr_uses_merge_base_not_new_main_changes(self):
        self.git("checkout", "-b", "codex/fixture")
        self.write("docs/pr.md")
        pr_head = self.commit()
        self.git("checkout", "main")
        self.write("main-only.kt")
        main_head = self.commit()
        result = self.classify(base=main_head, head=pr_head, event="pull_request")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("false", result.stdout.strip())


if __name__ == "__main__":
    unittest.main()
