set "ROBOZONKY_EXECUTABLE=${com.github.robozonky.distribution.jar}"

set "HERE=%~dp0%"
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

rem TODO Don't open java.util when Yasson can be upgraded to 1.0.8+, removing the warning.
set "ROBOZONKY_OPTS=%JAVA_OPTS% --add-opens java.base/java.util=ALL-UNNAMED -XX:+ExitOnOutOfMemoryError -Dlog4j.configurationFile=log4j2.xml -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
%JAVA_EXECUTABLE% %ROBOZONKY_OPTS% -jar %HERE%%ROBOZONKY_EXECUTABLE% %*

rem Don't let the window close if something went wrong.
pause
