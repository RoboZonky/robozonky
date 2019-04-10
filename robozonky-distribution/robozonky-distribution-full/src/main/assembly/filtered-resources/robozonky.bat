set "ROBOZONKY_OPTS=%JAVA_OPTS% -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager"
set "HERE=%~dp0%"
java %ROBOZONKY_OPTS% --module-path %HERE%\bin --module com.github.robozonky.app/com.github.robozonky.app.App %*
