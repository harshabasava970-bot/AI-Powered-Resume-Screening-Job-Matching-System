#!/bin/sh
# Maven Start Up Shell script (Linux/Mac)
MAVEN_PROJECTBASEDIR=$(dirname "$0")
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Maven wrapper..."
    curl -o "$WRAPPER_JAR" "$DOWNLOAD_URL" 2>/dev/null || wget -O "$WRAPPER_JAR" "$DOWNLOAD_URL" 2>/dev/null
fi

exec "$JAVA_HOME/bin/java" \
    -classpath "$WRAPPER_JAR" \
    "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
    $WRAPPER_LAUNCHER "$@"
