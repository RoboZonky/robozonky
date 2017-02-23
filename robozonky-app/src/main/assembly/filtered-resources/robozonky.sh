#!/bin/bash
ROBOZONKY_PORT=${ROBOZONKY_PORT:-7091}
ROBOZONKY_IP=${ROBOZONKY_IP:-localhost}
JMX_OPTS="-Djava.rmi.server.hostname=$ROBOZONKY_IP -Dcom.sun.management.jmxremote.port=$ROBOZONKY_PORT -Dcom.sun.management.jmxremote.rmi.port=$ROBOZONKY_PORT -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
ROBOZONKY_OPTS="$JMX_OPTS $JAVA_OPTS -Dlogback.configurationFile=logback.xml -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
$JAVA_HOME/bin/java $ROBOZONKY_OPTS -jar bin/robozonky-app-${project.version}.jar "$@"
