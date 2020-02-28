#!/bin/bash
export SOME_ENV_VAR="someValue"
export JAVA_OPTS="$JAVA_OPTS -Xmx512m --add-modules java.xml.bind -Dcom.github.someProperty=someValue"
/tmp/robozonky.sh @/tmp/robozonky-2986414312501048024.test