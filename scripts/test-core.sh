#!/usr/bin/env bash
# Compiles and runs the money-logic tests. Needs only a JDK (no Android SDK, no downloads).
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="$(mktemp -d)"
javac -Xlint:all -d "$OUT" app/src/main/java/com/techmare/paydebt/core/*.java tests/CoreTests.java
java -cp "$OUT" CoreTests
