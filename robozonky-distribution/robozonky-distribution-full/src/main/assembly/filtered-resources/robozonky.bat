set "ROBOZONKY_OPTS=%JAVA_OPTS%^
    -XX:+ExitOnOutOfMemoryError^
    -Dfile.encoding=UTF-8^
    -Djava.net.preferIPv4Stack=true^
    -XX:StartFlightRecording=disk=true,dumponexit=true,maxage=1d,path-to-gc-roots=true"
set "HERE=%~dp0%"
java %ROBOZONKY_OPTS% -jar %HERE%bin\robozonky-app-${project.version}.jar %*
