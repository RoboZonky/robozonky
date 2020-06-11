#!/bin/sh
ROBOZONKY_EXECUTABLE="${com.github.robozonky.distribution.jar}"

# Find the correct installation directory, regardless of whether it's from the ZIP or from the installer.
SCRIPT=$(realpath $0)
HERE=$(dirname $SCRIPT)
if [ -f $HERE/dist/$ROBOZONKY_EXECUTABLE ]; then
    HERE=$HERE/dist
fi
echo "Will run '$ROBOZONKY_EXECUTABLE' from '$HERE'."

# Use JRE bundled with RoboZonky, if available.
CUSTOM_JRE_LOCATION="$HERE/runtime"
JAVA_EXECUTABLE="java"

if [ -f $CUSTOM_JRE_LOCATION/bin/$JAVA_EXECUTABLE ]; then
    JAVA_EXECUTABLE=$CUSTOM_JRE_LOCATION/bin/$JAVA_EXECUTABLE
    JAVA_HOME=$CUSTOM_JRE_LOCATION
    echo "JAVA_HOME set to '$JAVA_HOME'."
fi
echo "Will use '$JAVA_EXECUTABLE' as the Java executable."

# TODO Don't open java.util when Yasson can be upgraded to 1.0.8+, removing the warning.
ROBOZONKY_OPTS="$JAVA_OPTS --add-opens java.base/java.util=ALL-UNNAMED -XX:+ExitOnOutOfMemoryError -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
# Added exec to propagate signals when Docker stop is issued, see https://github.com/RoboZonky/robozonky/pull/305.
exec $JAVA_EXECUTABLE $ROBOZONKY_OPTS -jar $HERE/$ROBOZONKY_EXECUTABLE "$@"
