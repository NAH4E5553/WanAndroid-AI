"""Install a checksum-pinned actionlint under this project's ignored build directory."""
import hashlib
import io
from pathlib import Path
import platform
import tarfile
from urllib.request import urlopen

VERSION = "1.7.7"
CHECKSUMS = {
    ("Darwin", "arm64"): ("darwin_arm64", "2693315b9093aeacb4ebd91a993fea54fc215057bf0da2659056b4bc033873db"),
    ("Darwin", "x86_64"): ("darwin_amd64", "28e5de5a05fc558474f638323d736d822fff183d2d492f0aecb2b73cc44584f5"),
    ("Linux", "x86_64"): ("linux_amd64", "023070a287cd8cccd71515fedc843f1985bf96c436b7effaecce67290e7e0757"),
    ("Linux", "aarch64"): ("linux_arm64", "401942f9c24ed71e4fe71b76c7d638f66d8633575c4016efd2977ce7c28317d0"),
}
ROOT = Path(__file__).resolve().parents[2]


def install_verified(payload, expected, destination):
    if hashlib.sha256(payload).hexdigest() != expected:
        raise ValueError("actionlint archive checksum mismatch")
    with tarfile.open(fileobj=io.BytesIO(payload), mode="r:gz") as archive:
        member = archive.getmember("actionlint")
        if not member.isfile() or member.size > 32 * 1024 * 1024:
            raise ValueError("invalid actionlint archive member")
        # Only read the named regular binary; never extract arbitrary paths or links.
        binary = archive.extractfile(member).read()
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix(".tmp")
    temporary.write_bytes(binary)
    temporary.chmod(0o755)
    temporary.replace(destination)


def main():
    artifact, checksum = CHECKSUMS[(platform.system(), platform.machine())]
    url = f"https://github.com/rhysd/actionlint/releases/download/v{VERSION}/actionlint_{VERSION}_{artifact}.tar.gz"
    with urlopen(url, timeout=30) as response:
        payload = response.read(32 * 1024 * 1024 + 1)
    if len(payload) > 32 * 1024 * 1024:
        raise ValueError("actionlint download exceeds size limit")
    destination = ROOT / "build/ci-tools/actionlint"
    install_verified(payload, checksum, destination)
    print(f"Installed actionlint {VERSION} with SHA-256 verification")


if __name__ == "__main__":
    main()
