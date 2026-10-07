#!/usr/bin/env python3
"""Rebuild only TermSession and its nested classes, preserving all other AAR content."""
from __future__ import annotations
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parent
BASE_SHA256 = "9be91343d611eacf1613583c11956fcd98baa637884ec0b6d41b53948346b991"


def pack(entries: dict[str, bytes]) -> bytes:
    output = io.BytesIO()
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_STORED) as z:
        for name, data in sorted(entries.items()):
            info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            z.writestr(info, data)
    return output.getvalue()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--android-jar", type=Path, required=True)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    original = ROOT / "upstream/emulatorview-1_0_42.aar"
    assert hashlib.sha256(original.read_bytes()).hexdigest() == BASE_SHA256
    with zipfile.ZipFile(original) as aar:
        entries = {n: aar.read(n) for n in aar.namelist() if not n.endswith("/")}
    javac = Path(os.environ["JAVA_HOME"]) / "bin" / ("javac.exe" if os.name == "nt" else "javac")
    sources = sorted((ROOT / "src").rglob("*.java"))
    with tempfile.TemporaryDirectory(prefix="shell-emulatorview-") as temp:
        work = Path(temp)
        base = work / "classes.jar"
        base.write_bytes(entries["classes.jar"])
        classes = work / "classes"
        classes.mkdir()
        subprocess.run([str(javac), "-encoding", "UTF-8", "-source", "8", "-target", "8", "-Xlint:-options", "-g",
                        "-classpath", os.pathsep.join([str(args.android_jar.resolve()), str(base)]),
                        "-d", str(classes), *map(str, sources)], check=True)
        with zipfile.ZipFile(base) as jar:
            bytecode = {n: jar.read(n) for n in jar.namelist() if not n.endswith("/") and
                        not (n.startswith("jackpal/androidterm/emulatorview/TermSession$") or
                             n == "jackpal/androidterm/emulatorview/TermSession.class")}
        bytecode.update({p.relative_to(classes).as_posix(): p.read_bytes() for p in classes.rglob("*.class")})
        entries["classes.jar"] = pack(bytecode)
    artifact = pack(entries)
    target = ROOT.parent.parent / "libs/jackpal/emulatorview-1_0_42.aar"
    provenance = {
        "upstreamCommit": "35188f8a8b57989a4a4ec9485e11187b46be26d9",
        "originalAarSha256": BASE_SHA256,
        "patchVersion": "1.0.42-p6.1",
        "compiler": subprocess.check_output([str(javac), "-version"], text=True).strip(),
        "androidJarSha256": hashlib.sha256(args.android_jar.read_bytes()).hexdigest(),
        "sources": {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},
        "aarSha256": hashlib.sha256(artifact).hexdigest(),
    }
    record = ROOT / "provenance.json"
    if args.check:
        assert target.read_bytes() == artifact, "Rebuilt emulatorview differs from the shipped artifact"
        assert json.loads(record.read_text()) == provenance, "Rebuild provenance differs"
    else:
        target.write_bytes(artifact)
        record.write_text(json.dumps(provenance, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(f"emulatorview {provenance['patchVersion']}: {provenance['aarSha256']}")


if __name__ == "__main__":
    main()
