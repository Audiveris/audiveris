#! /usr/bin/sh 
#---------------------------------------------------------------
#           convert-escaped-properties.sh
#---------------------------------------------------------------
# In all the .properties files found down the current location
# it replaces the escaped sequences (ISO-8859-1 ?)
# by UTF-8 characters
#
# Files are modified in place, after being saved in .bak files
#---------------------------------------------------------------

find . -type f -name "*.properties" -exec perl -i.bak -pe 's/\\u([0-9a-fA-F]{4})/chr(hex($1))/eg; s/\\x([0-9a-fA-F]{2})/chr(hex($1))/eg' {} +

# To clean up after inspection, we can use a command like:

# find . -type f -name "*.properties.bak" -delete
