set "ROBOZONKY_OPTS=%JAVA_OPTS% -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
set "HERE=%~dp0%"
java %ROBOZONKY_OPTS% --module-path %HERE%\bin --module com.github.robozonky.app %*
