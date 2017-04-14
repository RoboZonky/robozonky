set "ROBOZONKY_OPTS=-XX:+UseG1GC %JAVA_OPTS% -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
set "HERE=%~dp0%"
"%JAVA_HOME%\bin\java" %ROBOZONKY_OPTS% -jar %HERE%bin\robozonky-app-${project.version}.jar %*
