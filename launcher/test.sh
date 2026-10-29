#!/bin/sh
set -eu
cd "$(dirname "$0")"
TEST_BUILD=$(mktemp -d)
trap 'rm -rf "$TEST_BUILD"' EXIT HUP INT TERM
javac -d "$TEST_BUILD" src/org/cleantv/launcher/PickRules.java src/org/cleantv/launcher/PickEngine.java tests/PickRulesTest.java tests/PickEngineTest.java
java -cp "$TEST_BUILD" PickRulesTest
java -cp "$TEST_BUILD" PickEngineTest
