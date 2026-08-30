#!/bin/sh

APP_HOME=$( cd "${0%/*}" && pwd -P ) || exit

exec java -Dorg.gradle.appname=gradlew -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
