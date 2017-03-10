#!/bin/bash
BASEDIR=$(dirname "$0")
ROBOZONKY_OPTS="$JAVA_OPTS -Dlogback.configurationFile=logback.xml -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
$JAVA_HOME/bin/java $ROBOZONKY_OPTS -jar ${BASEDIR}/bin/robozonky-app-${project.version}.jar "$@"
