#!/bin/sh

# check for Java 7 and bail out if not available
JAVA_VERSION=$(java -version 2>&1 | sed 's/java version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "This version of Audiveris requires Java 7. Please install a Java 7 JRE and try again."
    exit 1
fi

PROG_DIR=/usr/share/audiveris
EXEC=$PROG_DIR/dist/audiveris.jar

# Run Audiveris
LC_ALL=C java -jar $EXEC "$@"

