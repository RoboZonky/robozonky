#!/bin/bash
BASEDIR=$(dirname "$0")
$JAVA_HOME/bin/java -Xmx64m -jar $BASEDIR/zonkybot-app-${project.version}.jar "$@"
