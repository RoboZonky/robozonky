set "JAVA_OPTS=%JAVA_OPTS% -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false"
set "JAVA_OPTS=%JAVA_OPTS% -Dlogback.configurationFile=logback.xml -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
java %JAVA_OPTS% -jar robozonky-app-${project.version}.jar %*
