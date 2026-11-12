#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
self="scripts/$(basename "${BASH_SOURCE[0]}")"
cd "$root"

patterns=(
  -e 'capcom'
  -e 'sms-gate'
  -e 'smsgate'
  -e 'SMS Gateway for Android'
  -e 'me\.capcom'
)

matches="$(
  grep -rniI "${patterns[@]}" . \
    --exclude-dir=.git \
    --exclude-dir=build \
    --exclude-dir=.gradle \
    --exclude-dir=.kotlin \
    --exclude-dir=.idea \
    --exclude-dir=captures \
    --exclude-dir=.cxx \
    --exclude-dir=.externalNativeBuild \
    | grep -v "^\./${self}:" || true
)"

binary_matches="$(
  grep -rlia "${patterns[@]}" . \
    --exclude-dir=.git \
    --exclude-dir=build \
    --exclude-dir=.gradle \
    --exclude-dir=.kotlin \
    --exclude-dir=.idea \
    --exclude-dir=captures \
    --exclude-dir=.cxx \
    --exclude-dir=.externalNativeBuild \
    | grep -v "^\./${self}$" || true
)"

name_matches="$(
  find . \( -name .git -o -name build -o -name .gradle -o -name .kotlin -o -name .idea \) -prune -o \
    \( -iname '*capcom*' -o -iname '*sms-gate*' -o -iname '*smsgate*' \) -print
)"

if [[ -n "$matches" || -n "$binary_matches" || -n "$name_matches" ]]; then
  echo "Legacy references found:" >&2
  [[ -n "$matches" ]] && echo "$matches" >&2
  [[ -n "$binary_matches" ]] && echo "$binary_matches" | sed 's/^/file: /' >&2
  [[ -n "$name_matches" ]] && echo "$name_matches" | sed 's/^/path: /' >&2
  exit 1
fi

echo "No legacy references found."
