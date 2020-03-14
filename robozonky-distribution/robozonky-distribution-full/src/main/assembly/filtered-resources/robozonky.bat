set "ROBOZONKY_EXECUTABLE=robozonky-distribution-full-${project.version}.jar"

rem Find the correct installation directory, regardless of whether it's from the ZIP or from the installer.
set "HERE=%~dp0%"
IF EXIST %HERE%Dist\%ROBOZONKY_EXECUTABLE% (
    set "HERE=%HERE%Dist\"
)
echo Will run %ROBOZONKY_EXECUTABLE% from '%HERE%'.

rem Use JRE bundled with RoboZonky, if available.
set "CUSTOM_JRE_LOCATION=%HERE%jre\"
set "JAVA_EXECUTABLE=java"
IF EXIST %CUSTOM_JRE_LOCATION%bin\%JAVA_EXECUTABLE% (
    set "JAVA_EXECUTABLE=%CUSTOM_JRE_LOCATION%bin\%JAVA_EXECUTABLE%"
    set "JAVA_HOME=%CUSTOM_JRE_LOCATION%"
)
echo Will use '%JAVA_EXECUTABLE%' as the Java executable.
echo JAVA_HOME set to '%JAVA_HOME%'.

set "ROBOZONKY_OPTS=%JAVA_OPTS% -XX:+ExitOnOutOfMemoryError -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
%JAVA_EXECUTABLE% %ROBOZONKY_OPTS% -jar %HERE%%ROBOZONKY_EXECUTABLE% %*
