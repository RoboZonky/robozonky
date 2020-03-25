#!/bin/sh

if [ $# -ne 2 ]; then
    echo "Expecting two arguments, the JAR and the target directory."
    exit 1
fi

INPUT_JAR=$1
TARGET_DIR=$2

echo "Will create an executable for '$INPUT_JAR'."
echo "Will store the executable in directory '$TARGET_DIR'."

$JAVA_HOME/bin/java -version

# Retrieve a list of required JDK modules from the input JAR.
JDEPS_CMD="$JAVA_HOME/bin/jdeps --multi-release 11 --print-module-deps --ignore-missing-deps $INPUT_JAR"
echo "Calling jdeps like so: "
echo "  $JDEPS_CMD"
DEPENDENCIES=$($JDEPS_CMD)
echo "jdeps returned:"
echo "$DEPENDENCIES"

# Call JLink with these dependencies; add locales and crypto, also JMX and JFR on top, as those are runtime monitoring dependencies.
# We could use --bind-services instead, but that makes the runtime huge and includes stuff like javac etc.
JLINK_CMD="$JAVA_HOME/bin/jlink --compress=2 --no-header-files --no-man-pages
   --strip-native-debug-symbols=exclude-debuginfo-files
   --dedup-legal-notices=error-if-not-same-content
   --include-locales=en,cs
   --add-modules $DEPENDENCIES,jdk.management.jfr,jdk.management.agent,jdk.crypto.ec,jdk.localedata
   --output $TARGET_DIR"
echo "Calling jlink like so: "
echo "  $JLINK_CMD"
JLINK_RESULT=$($JLINK_CMD)
echo "jlink returned:"
echo "$JLINK_RESULT"

echo "Finished."
