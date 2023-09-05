#!/usr/bin/bash

#---------------------------------------------------------------------------------------------------
#                                     p d f - b u i l d . s h
#---------------------------------------------------------------------------------------------------
# Generation of a PDF version of Audiveris handbook.
#
# This is derived from the file hamoid / justTheDocsToPDF.bash
# found at https://gist.github.com/hamoid
#---------------------------------------------------------------------------------------------------
#
# This bash file has been tested on Windows/Cygwin. It should also run on Linux and MacOS.
# It uses standard utilities: curl, grep, head, perl, sed, tail, echo, cat
#
# It also uses the "prince" software which is assumed to be found on PATH.
# Prince can be downloaded from https://www.princexml.com/download/ for various OSes
# and can be freely used for non-commercial use.
#---------------------------------------------------------------------------------------------------
#
# This file is meant to be run from docs/pdf folder.
#
# If the optional "local" parameter is provided, HTML content is retrieved from
# a local generator found at http://localhost:4000
# Otherwise, it is retrieved from GitHub Audiveris at https://audiveris.github.io
#
# Path to the resulting file is docs/pdf/build/Audiveris_Handbook.pdf
#---------------------------------------------------------------------------------------------------

# Variables
#----------
if [ "$1" = "local" ]
then
    PREFIX="http://localhost:4000"
else
    PREFIX="https://audiveris.github.io"
fi
echo "PREFIX:" $PREFIX

# House keeping
#--------------
if [ -d build ]
then
    rm build/*
else
    mkdir build
fi

# Populate core.html
#-------------------

# 1/ curl retrieves HTML content from local or remote site
# 2/ grep retrieves all <nav> ... </nav> sections (there are 2 such sections)
# 3/ head picks up just the first <nav> ... </nav> section
# 4/ perl removes all <button> ... </button> sections
# 5/ sed adjusts href values
curl -sk $PREFIX/audiveris/_pages/handbook/ |\
grep -o -P '<nav .*?</nav>' |\
head -1 |\
perl -pe 's|<button .*?</button>||g' |\
sed -E "s|/audiveris/_pages/|$PREFIX/audiveris/_pages/|g" > build/core.html

# Populate catalog.txt
#---------------------

echo "build/nav.html" > build/catalog.txt

# Retrieve all urls:
# 1/ sed injects a line break in front of every URL PREFIX
# 2/ sed deletes from each line the " character and everything that follows, leaving the clean URL
# 3/ tail deletes the first line, which contains a lonely <nav> tag
sed "s|$PREFIX|\n$PREFIX|g" build/core.html | sed "s/\".*//g" | tail +2 >> build/catalog.txt

# Populate nav.html
#------------------

echo "<!DOCTYPE html>" > build/nav.html
echo "<html>" >> build/nav.html
echo "<head>" >> build/nav.html
echo '<link rel="stylesheet" href="../pdf-nav-style.css"/>' >> build/nav.html
echo "</head>" >> build/nav.html
echo "<body>" >> build/nav.html

cat build/core.html >> build/nav.html

echo "</body>" >> build/nav.html
echo "</html>" >> build/nav.html

# Use Prince to build the PDF
#----------------------------

prince --input-list=build/catalog.txt -o build/Audiveris_Handbook.pdf