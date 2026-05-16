#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

PORT="${1:-8000}"

mkdir -p backend/build/classes
javac -d backend/build/classes backend/src/main/java/*.java
java -cp backend/build/classes WebApp "$PORT"
