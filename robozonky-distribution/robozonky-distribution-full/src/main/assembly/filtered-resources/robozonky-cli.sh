#!/bin/sh
#
# Copyright 2021 The RoboZonky Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

ROBOZONKY_EXECUTABLE="${com.github.robozonky.distribution.jar.cli}"

# Find the correct installation directory, regardless of whether it's from the ZIP or from the installer.
HERE=$(dirname $0)
if [ -f $HERE/dist/$ROBOZONKY_EXECUTABLE ]; then
    HERE=$HERE/dist
fi
# echo "Will run '$ROBOZONKY_EXECUTABLE' from '$HERE'."

# Use Java runtime bundled with RoboZonky, if available.
CUSTOM_JRE_LOCATION="$HERE/runtime"
JAVA_EXECUTABLE="java"

if [ -f $CUSTOM_JRE_LOCATION/bin/$JAVA_EXECUTABLE ]; then
    JAVA_EXECUTABLE=$CUSTOM_JRE_LOCATION/bin/$JAVA_EXECUTABLE
    JAVA_HOME=$CUSTOM_JRE_LOCATION
    # echo "JAVA_HOME set to '$JAVA_HOME'."
fi
# echo "Will use '$JAVA_EXECUTABLE' as the Java executable."

ROBOZONKY_OPTS="$JAVA_OPTS -XX:+ExitOnOutOfMemoryError -Duser.timezone=Europe/Prague -Dlog4j.configurationFile=log4j2.xml -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
# Added exec to propagate signals when Docker stop is issued, see https://github.com/RoboZonky/robozonky/pull/305.
exec $JAVA_EXECUTABLE $ROBOZONKY_OPTS -jar $HERE/$ROBOZONKY_EXECUTABLE "$@"
