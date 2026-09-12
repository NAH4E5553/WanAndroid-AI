"""Exercise the real Gradle test graph and fail-closed guard without changing project sources."""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]


def gradle(*args):
    return subprocess.run(
        [str(ROOT / "gradlew"), "testAll", "--dry-run", "--no-configuration-cache",
         "--no-daemon", *args], cwd=ROOT, text=True,
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=300,
    )


def verify():
    result = gradle()
    if result.returncode:
        raise RuntimeError(result.stdout)
    for task in (
        ":app:testDebugUnitTest", ":core:result:test", ":core:common:testDebugUnitTest",
        ":core:data:testDebugUnitTest", ":core:navigation:testDebugUnitTest",
        ":feature:home:testDebugUnitTest", ":feature:profile:testDebugUnitTest",
    ):
        if f"{task} SKIPPED" not in result.stdout:
            raise AssertionError(f"testAll does not include {task}\n{result.stdout}")

    scratch = ROOT / "build/ci-tests"
    scratch.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="test-aggregation-", dir=scratch) as directory:
        fixture = Path(directory)
        # A real Gradle module using a platform not covered by testAll must be rejected.
        (fixture / "build.gradle").write_text("plugins { id 'base' }\n")
        init = fixture / "probe.init.gradle"
        init.write_text("""gradle.settingsEvaluated { settings ->
    if (settings.rootDir.canonicalFile != new File(System.getProperty('wan.coverageRoot')).canonicalFile) return
    settings.include(':feature:coverageProbe')
    settings.project(':feature:coverageProbe').projectDir = new File(
        settings.rootDir, System.getProperty('wan.coverageProbeDir'))
}
""")
        result = gradle(
            "--init-script", str(init), f"-Dwan.coverageRoot={ROOT}",
            f"-Dwan.coverageProbeDir={fixture.relative_to(ROOT)}",
        )
        expected = "Unsupported test platform: :feature:coverageProbe"
        if result.returncode == 0 or expected not in result.stdout:
            raise AssertionError(f"Unsupported module did not fail with the expected guard\n{result.stdout}")
    print("testAll: Android/JVM tasks included; unsupported platform rejected.")


if __name__ == "__main__":
    verify()
