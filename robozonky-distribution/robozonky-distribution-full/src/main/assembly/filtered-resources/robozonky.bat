set "ROBOZONKY_OPTS=%JAVA_OPTS% -XX:+ExitOnOutOfMemoryError -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
set "HERE=%~dp0%"
java %ROBOZONKY_OPTS% --module-path %HERE%\bin --module com.github.robozonky.app %*
