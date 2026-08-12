#!/usr/bin/env bash
# Gradle wrapper with a JDK on PATH (host has no system java).
set -euo pipefail
export JAVA_HOME=/nix/store/i4y48fdcc99kzw28f9mm4102ji1kx9mx-openjdk-21.0.12+2
export PATH="$JAVA_HOME/bin:$PATH"
cd "$(dirname "$0")"
exec ./gradlew "$@"
