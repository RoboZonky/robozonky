#!/bin/sh
HERE=$(dirname "$0")
if [ -f ${HERE}/Dist/bin/robozonky-app-${project.version}.jar ]; then
    HERE=$HERE/Dist
fi
ROBOZONKY_OPTS="$JAVA_OPTS -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
#added exec to propagate signals when docker stop is issued, more details https://github.com/RoboZonky/robozonky/pull/305
exec java $ROBOZONKY_OPTS -jar ${HERE}/bin/robozonky-app-${project.version}.jar "$@"
