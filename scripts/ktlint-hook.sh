#!/usr/bin/env bash
# scripts/ktlint-hook.sh
# Runs ktlint --format on staged Kotlin files.
# Called by the android-ktlint pre-commit hook.
#
# pre-commit passes staged file paths as arguments.
# The jar path is resolved relative to the repo root
# so it works regardless of where pre-commit is invoked from.

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
JAR="$REPO_ROOT/.gradle/ktlint/ktlint-1.3.1.jar"

if [ ! -f "$JAR" ]; then
  echo "ktlint jar not found at $JAR"
  echo "Run: bash scripts/download-ktlint.sh"
  exit 1
fi

java -jar "$JAR" --format "$@"
