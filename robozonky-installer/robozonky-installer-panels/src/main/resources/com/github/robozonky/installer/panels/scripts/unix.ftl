#!/bin/bash
<#list data.envVars as var, value>export ${var}="${value}"
</#list>
export JAVA_OPTS="$JAVA_OPTS ${data.javaOpts}"
${data.script} @${data.options}
