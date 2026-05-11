#!/usr/bin/env bash
# scripts/download-ktlint.sh
#
# Downloads the ktlint standalone jar used by the pre-commit hook.
# Run once after cloning:
#   bash scripts/download-ktlint.sh
#
# The jar is cached at .gradle/ktlint/ktlint-1.3.1.jar (gitignored via
# the existing android/.gitignore which excludes .gradle/).

set -euo pipefail

KTLINT_VERSION="1.3.1"
CACHE_DIR=".gradle/ktlint"
JAR_PATH="$CACHE_DIR/ktlint-$KTLINT_VERSION.jar"
DOWNLOAD_URL="https://github.com/pinterest/ktlint/releases/download/$KTLINT_VERSION/ktlint"

if [ -f "$JAR_PATH" ]; then
  echo "✓ ktlint $KTLINT_VERSION already at $JAR_PATH"
  exit 0
fi

echo "→ Downloading ktlint $KTLINT_VERSION..."
mkdir -p "$CACHE_DIR"
curl -sSL "$DOWNLOAD_URL" -o "$JAR_PATH"
echo "✓ Saved to $JAR_PATH"

# Smoke-test: verify java can load the jar
java -jar "$JAR_PATH" --version
echo "✓ ktlint ready"
