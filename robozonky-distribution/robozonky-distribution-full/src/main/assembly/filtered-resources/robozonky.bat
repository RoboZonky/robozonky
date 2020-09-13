set "ROBOZONKY_EXECUTABLE=${com.github.robozonky.distribution.jar}"

rem Find the correct installation directory, regardless of whether it's from the ZIP or from the installer.
set "HERE=%~dp0%"
IF EXIST %HERE%dist\%ROBOZONKY_EXECUTABLE% (
    set "HERE=%HERE%dist\"
)
echo Will run %ROBOZONKY_EXECUTABLE% from '%HERE%'.

rem Use Java runtime bundled with RoboZonky, if available.
set "CUSTOM_JRE_LOCATION=%HERE%runtime\"
set "JAVA_EXECUTABLE=java.exe"
IF EXIST %CUSTOM_JRE_LOCATION%bin\%JAVA_EXECUTABLE% (
    set "JAVA_EXECUTABLE=%CUSTOM_JRE_LOCATION%bin\%JAVA_EXECUTABLE%"
    set "JAVA_HOME=%CUSTOM_JRE_LOCATION%"
)
echo Will use '%JAVA_EXECUTABLE%' as the Java executable.
echo JAVA_HOME set to '%JAVA_HOME%'.

set "ROBOZONKY_OPTS=%JAVA_OPTS% -XX:+ExitOnOutOfMemoryError -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
"%JAVA_EXECUTABLE%" %ROBOZONKY_OPTS% -jar "%HERE%%ROBOZONKY_EXECUTABLE%" %*

rem Don't let the window close if something went wrong.
pause
