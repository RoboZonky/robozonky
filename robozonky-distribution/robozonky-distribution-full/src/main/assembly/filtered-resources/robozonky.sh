#!/bin/bash
HERE=$(dirname "$0")
if [ -f ${HERE}/Dist/bin/robozonky-app-${project.version}.jar ]; then
    HERE=$HERE/Dist
fi
ROBOZONKY_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
exec java $ROBOZONKY_OPTS -jar ${HERE}/bin/robozonky-app-${project.version}.jar "$@"
