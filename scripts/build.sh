#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

cd frontend
npm install
npm run build
cd ..

mkdir -p backend/build/classes
javac -d backend/build/classes backend/src/main/java/*.java
