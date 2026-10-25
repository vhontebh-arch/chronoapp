#!/usr/bin/env sh
# ChronoDeck Gradle bootstrap. Uses the official Gradle Wrapper when its JAR is present;
# otherwise downloads the pinned Gradle distribution and runs it directly.
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=8.11.1
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ -f "$WRAPPER_JAR" ]; then
  exec java ${JAVA_OPTS:-} ${GRADLE_OPTS:-} -classpath "$WRAPPER_JAR" -Dorg.gradle.appname=gradlew org.gradle.wrapper.GradleWrapperMain "$@"
fi
GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}/chronodeck-bootstrap/gradle-$GRADLE_VERSION"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/chronodeck-bootstrap"
  mkdir -p "$BASE"
  ZIP="$BASE/gradle-$GRADLE_VERSION-bin.zip"
  if [ ! -f "$ZIP" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    else
      echo "ChronoDeck: install curl or wget to download Gradle." >&2; exit 1
    fi
  fi
  command -v unzip >/dev/null 2>&1 || { echo "ChronoDeck: unzip is required." >&2; exit 1; }
  unzip -q -o "$ZIP" -d "$BASE"
fi
exec "$GRADLE_HOME/bin/gradle" "$@"
