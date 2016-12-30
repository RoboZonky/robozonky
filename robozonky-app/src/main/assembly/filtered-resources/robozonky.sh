#!/bin/bash
BASEDIR=$(dirname "$0")
JAVA_OPTS="$JAVA_OPTS -Dlogback.configurationFile=logback.xml -Dfile.encoding="UTF-8" -Djava.net.preferIPv4Stack=true"
$JAVA_HOME/bin/java $JAVA_OPTS -jar $BASEDIR/robozonky-app-${project.version}.jar "$@"
