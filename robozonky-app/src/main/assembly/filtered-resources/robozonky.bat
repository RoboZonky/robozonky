if not defined ROBOZONKY_PORT set ROBOZONKY_PORT=7091
if not defined ROBOZONKY_IP set ROBOZONKY_IP=localhost
set "JMX_OPTS=-Djava.rmi.server.hostname=%ROBOZONKY_IP% -Dcom.sun.management.jmxremote.port=%ROBOZONKY_PORT% -Dcom.sun.management.jmxremote.rmi.port=%ROBOZONKY_PORT% -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
set "ROBOZONKY_OPTS=%JMX_OPTS% %JAVA_OPTS% -Dlogback.configurationFile=logback.xml -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
java %ROBOZONKY_OPTS% -jar bin/robozonky-app-${project.version}.jar @robozonky.cli
