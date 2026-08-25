#!/usr/bin/env bash
# Verifies that every LOAD segment of the given shared objects is aligned to
# at least 16 KB — Android 15+ requirement for devices with 16 KB pages.
#
# Usage: check_elf_alignment.sh <path-to-llvm-readelf> <so-file> [<so-file> ...]
set -euo pipefail

if [[ $# -lt 2 ]]; then
    echo "usage: $0 <llvm-readelf> <file.so> [...]" >&2
    exit 2
fi

READELF="$1"
shift

REQUIRED=16384
FAILED=0

for SO in "$@"; do
    BAD_ALIGNMENTS="$(
        "$READELF" -lW "$SO" |
        awk '/ LOAD / { if (strtonum($NF) < '"$REQUIRED"') print $NF }'
    )"
    if [[ -n "$BAD_ALIGNMENTS" ]]; then
        echo "FAIL  $SO (LOAD alignment below ${REQUIRED}: $BAD_ALIGNMENTS)"
        FAILED=1
    else
        echo "OK    $SO"
    fi
done

exit "$FAILED"
