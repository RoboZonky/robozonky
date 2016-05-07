#!/bin/bash
BASEDIR=$(dirname "$0")
$JAVA_HOME/bin/java -Dfile.encoding="UTF-8" -Xmx64m -jar $BASEDIR/zonkybot-app-${project.version}.jar "$@"
