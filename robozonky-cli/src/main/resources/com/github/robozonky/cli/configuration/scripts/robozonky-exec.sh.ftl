#!/bin/bash
export JAVA_OPTS="$JAVA_OPTS ${data.javaOpts}"
${data.root}/robozonky.sh @${data.options}
