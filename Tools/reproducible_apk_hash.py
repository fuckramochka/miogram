#!/usr/bin/env python3
"""Deterministic content hash of an APK/AAB.

Re-zips all entries in sorted order with fixed timestamps and compression,
excluding signing metadata (META-INF/*). Two builds of identical sources
produce identical hashes even when zip entry order, timestamps or the debug
signing key differ.

Usage: reproducible_apk_hash.py <apk> [<apk> ...]
Prints "<sha256>  <filename>" per input.
"""
import hashlib
import io
import sys
import zipfile


FIXED_DATE = (1980, 1, 1, 0, 0, 0)
EXCLUDED_PREFIXES = ("META-INF/",)


def canonical_digest(path: str) -> str:
    buffer = io.BytesIO()
    with zipfile.ZipFile(path, "r") as source:
        names = sorted(
            info.filename
            for info in source.infolist()
            if not info.is_dir()
            and not info.filename.startswith(EXCLUDED_PREFIXES)
        )
        with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as target:
            for name in names:
                payload = source.read(name)
                entry = zipfile.ZipInfo(name, date_time=FIXED_DATE)
                entry.compress_type = zipfile.ZIP_DEFLATED
                entry.external_attr = 0o644 << 16
                target.writestr(entry, payload)
    return hashlib.sha256(buffer.getvalue()).hexdigest()


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__, file=sys.stderr)
        return 2
    hashes = []
    for apk in argv[1:]:
        digest = canonical_digest(apk)
        hashes.append(digest)
        print(f"{digest}  {apk}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
