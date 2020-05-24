#!/bin/sh
ROBOZONKY_EXECUTABLE="${com.github.robozonky.distribution.jar.cli}"

# Find the correct installation directory, regardless of whether it's from the ZIP or from the installer.
SCRIPT=$(realpath $0)
HERE=$(dirname $SCRIPT)
if [ -f $HERE/Dist/$ROBOZONKY_EXECUTABLE ]; then
    HERE=$HERE/Dist
fi
echo "Will run '$ROBOZONKY_EXECUTABLE' from '$HERE'."

# Use JRE bundled with RoboZonky, if available.
CUSTOM_JRE_LOCATION="$HERE/jre"
JAVA_EXECUTABLE="java"

if [ -f $CUSTOM_JRE_LOCATION/bin/$JAVA_EXECUTABLE ]; then
    JAVA_EXECUTABLE=$CUSTOM_JRE_LOCATION/bin/$JAVA_EXECUTABLE
    JAVA_HOME=$CUSTOM_JRE_LOCATION
fi
echo "Will use '$JAVA_EXECUTABLE' as the Java executable."
echo "JAVA_HOME set to '$JAVA_HOME'."

ROBOZONKY_OPTS="$JAVA_OPTS -XX:+ExitOnOutOfMemoryError -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
# Added exec to propagate signals when Docker stop is issued, see https://github.com/RoboZonky/robozonky/pull/305.
exec $JAVA_EXECUTABLE $ROBOZONKY_OPTS -jar $HERE/$ROBOZONKY_EXECUTABLE "$@"
