#!/bin/bash
HERE=$(dirname "$0")
ROBOZONKY_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
$JAVA_HOME/bin/java $ROBOZONKY_OPTS -jar ${HERE}/bin/robozonky-app-${project.version}.jar "$@"
