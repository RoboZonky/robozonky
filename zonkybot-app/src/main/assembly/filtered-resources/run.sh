#!/bin/bash
BASEDIR=$(dirname "$0")
java -Xmx64m -jar $BASEDIR/zonkybot-app-${project.version}.jar "$@"
