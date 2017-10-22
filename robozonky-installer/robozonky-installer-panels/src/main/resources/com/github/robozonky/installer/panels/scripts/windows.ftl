<#list data.envVars as var, value>set "${var}=${value}"
</#list>
set "JAVA_OPTS=%JAVA_OPTS% ${data.javaOpts}"
${data.root}\robozonky.bat @${data.options}
