#!/usr/bin/env bash
# Adapted from CoolMallKotlin's conservative change classifier; see NOTICE.md.
set -euo pipefail

if [[ "$#" -ne 3 ]]; then
  echo "usage: $0 <base-sha> <head-sha> <push|pull_request>" >&2
  exit 2
fi
base_sha="$1"
head_sha="$2"
event_name="$3"

for sha in "$base_sha" "$head_sha"; do
  if [[ ! "$sha" =~ ^[0-9a-fA-F]{40}$ && ! "$sha" =~ ^[0-9a-fA-F]{64}$ ]]; then
    echo "invalid commit SHA" >&2
    exit 2
  fi
  git cat-file -e "${sha}^{commit}"
done
case "$event_name" in
  pull_request) base_sha="$(git merge-base "$base_sha" "$head_sha")" ;;
  push) ;;
  *) echo "unsupported event" >&2; exit 2 ;;
esac

classify_paths() {
  local path
  local has_changes=false
  local run_android=false
  while IFS= read -r -d '' path; do
    has_changes=true
    case "$path" in
      AGENTS.md | */AGENTS.md)
        run_android=true
        ;;
      README.md | NOTICE.md | docs/*.md | .github/*.md)
        # Known documentation only. OCR rules and CI configuration are NOT skipped.
        ;;
      *)
        run_android=true
        ;;
    esac
  done
  if [[ "$has_changes" != "true" ]]; then
    echo "cannot classify an empty change range" >&2
    return 1
  fi
  printf '%s\n' "$run_android"
}

# --no-renames preserves BOTH paths: renaming code into docs must still run Android.
# pipefail propagates git errors; NUL separation safely handles spaces/newlines in paths.
git diff --no-ext-diff --no-renames --name-only -z "$base_sha" "$head_sha" -- | classify_paths
