#!/bin/sh

# check for Java 7 and bail out if not available
JAVA_VERSION=$(java -version 2>&1 | sed 's/java version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "This version of Audiveris requires Java 7. Please install a Java 7 JRE and try again."
    exit 1
fi

PROG_DIR=$(dirname $0)/AudiverisApp/
EXEC=$PROG_DIR/dist/audiveris.jar

# Select JNI library according with the host hardware architecture
if [ `getconf LONG_BIT` = "64" ]
then
    echo "64bit TessBridge JNI has been activated"
    JNI_DIR=$PROG_DIR/TessBridgeJNI/64bit/
else
    echo "32bit TessBridge JNI has been activated"
    JNI_DIR=$PROG_DIR/TessBridgeJNI/32bit/
fi

# Run Audiveris
LC_ALL=C java -Djava.library.path=$JNI_DIR -jar $EXEC "$@"

