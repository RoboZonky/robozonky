set "ROBOZONKY_OPTS=-XX:+UseG1GC %JAVA_OPTS% -Dlogback.configurationFile=logback.xml -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"
java %ROBOZONKY_OPTS% -jar bin/robozonky-app-${project.version}.jar %*
