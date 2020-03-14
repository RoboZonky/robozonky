#!/bin/sh

if [ $# -ne 2 ]; then
    echo "Expecting two arguments, the JAR and the target directory."
    exit 1
fi

INPUT_JAR=$1
TARGET_DIR=$2

echo "Will create an executable for '$INPUT_JAR'."
echo "Will store the executable in directory '$TARGET_DIR'."

java -version

# Retrieve a list of required JDK modules from the input JAR.
JDEPS_CMD="jdeps --print-module-deps --ignore-missing-deps $INPUT_JAR"
echo "Calling jdeps like so: "
echo "  $JDEPS_CMD"
DEPENDENCIES=$($JDEPS_CMD)
echo "jdeps returned:"
echo "$DEPENDENCIES"

# Call JLink with these dependencies.
JLINK_CMD="jlink --compress=2 --no-header-files --no-man-pages --strip-debug --add-modules $DEPENDENCIES --output $TARGET_DIR"
echo "Calling jlink like so: "
echo "  $JLINK_CMD"
JLINK_RESULT=$($JLINK_CMD)
echo "jlink returned:"
echo "$JLINK_RESULT"

echo "Finished."
