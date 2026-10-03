#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
java com.sun.tools.javac.Main -d "$out" app/src/main/java/kr/watchlite/Core.java tests/CoreTest.java
java -cp "$out" kr.watchlite.CoreTest
