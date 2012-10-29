#!/bin/sh

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

