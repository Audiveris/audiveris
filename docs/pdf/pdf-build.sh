#!/usr/bin/bash

#---------------------------------------------------------------------------------------------------
#                                     p d f - b u i l d . s h
#---------------------------------------------------------------------------------------------------
# Generation of a PDF version of Audiveris HANDBOOK.
#
# This is derived from the file hamoid / justTheDocsToPDF.bash
# found at https://gist.github.com/hamoid
#---------------------------------------------------------------------------------------------------
#
# This bash file has been tested on Windows/Cygwin. It should also run on Linux and macOS.
# It uses standard utilities: curl, grep, head, perl, sed, tail, echo, cat
#
# It also uses the "prince" software which is assumed to be found on PATH.
# Prince can be downloaded from https://www.princexml.com/download/ for various OSes
# and can be freely used for non-commercial use.
#---------------------------------------------------------------------------------------------------
#
# This file can be run directly from the 'app' project folder
# or preferably via the Gradle task "handbookPdf"
#
# If the optional "local" parameter is provided, HTML content is retrieved from
# a local generator found at http://localhost:4000
# Otherwise, it is retrieved from GitHub Audiveris at https://audiveris.github.io
#
# Path to the resulting file is build/pdf/Audiveris_Handbook.pdf
#---------------------------------------------------------------------------------------------------

# Variables
#----------
if [ "$1" = "localhost" ]
then
    PREFIX="http://localhost:4000"
else
    PREFIX="https://audiveris.github.io"
fi
echo "PREFIX:" $PREFIX

# House keeping
#--------------
TARGET="build/pdf"
CORE="$TARGET/core.html"
CATALOG="$TARGET/catalog.txt"
NAV="$TARGET/nav.html"
HANDBOOK="$TARGET/Audiveris_Handbook.pdf"
STYLE="../../../docs/pdf/pdf-nav-style.css"

mkdir -p $TARGET

# Populate CORE
#--------------
# 1/ curl retrieves HTML content from local or remote site
# 2/ grep retrieves all <NAV> ... </NAV> sections (there are 2 such sections)
# 3/ head picks up just the first <NAV> ... </NAV> section
# 4/ perl removes all <button> ... </button> sections
# 5/ sed adjusts href values
curl -sk $PREFIX/audiveris/_pages/handbook/ |\
grep -o -P '<nav .*?</nav>' |\
head -1 |\
perl -pe 's|<button .*?</button>||g' |\
sed -E "s|/audiveris/_pages/|$PREFIX/audiveris/_pages/|g" > $CORE

# Populate catalog.txt
#---------------------

# Populate CATALOG (navigation url, then all pages urls)
#-----------------
echo "$NAV" > $CATALOG

# Retrieve all urls:
# 1/ sed injects a line break in front of every URL PREFIX
# 2/ sed deletes from each line the " character and everything that follows, leaving the clean URL
# 3/ tail deletes the first line, which contains a lonely <NAV> tag
sed "s|$PREFIX|\n$PREFIX|g" $CORE | sed "s/\".*//g" | tail +2 >> $CATALOG

# Populate NAV (header with proper style sheet for navigation, then core stuff)
#-------------
echo "<!DOCTYPE html>" > $NAV
echo "<html>" >> $NAV
echo "<head>" >> $NAV
echo "<title>Audiveris HandBook</title>" >> $NAV
echo "<link rel='stylesheet' href='$STYLE'/>" >> $NAV
echo "</head>" >> $NAV
echo "<body>" >> $NAV

cat $CORE >> $NAV

echo "</html>" >> $NAV

# Use Prince to build the PDF
#----------------------------
prince --input-list=$CATALOG -o $HANDBOOK