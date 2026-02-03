#!/usr/bin/env bash
set -euo pipefail

# Small helper to run the app with Temurin-17 (if installed) and load .env
DIR="$(cd "$(dirname "$0")" && pwd)"

# Try to set JAVA_HOME if not already set
if [ -z "${JAVA_HOME-}" ]; then
  if [ -d /usr/lib/jvm ]; then
    CAND=$(ls -1 /usr/lib/jvm | grep -i temurin | grep 17 || true)
    if [ -n "$CAND" ]; then
      export JAVA_HOME="/usr/lib/jvm/$CAND"
    fi
  fi
fi

if [ -n "${JAVA_HOME-}" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
  echo "Using JAVA_HOME=$JAVA_HOME"
else
  echo "WARN: JAVA_HOME not set and Temurin-17 not detected; ensure your shell uses Java 17." >&2
fi

# Load .env from repository root (one level up from backend)
ENV_ROOT="$(dirname "$DIR")/.env"
if [ -f "$ENV_ROOT" ]; then
  echo "Loading $ENV_ROOT"
  set -a
  # shellcheck disable=SC1090
  source "$ENV_ROOT"
  set +a
fi

cd "$DIR"
echo "Starting Spring Boot..."
mvn -U spring-boot:run
