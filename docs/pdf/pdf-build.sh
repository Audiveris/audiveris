#!/usr/bin/bash

#---------------------------------------------------------------------------------
#                            p d f - b u i l d . s h
#---------------------------------------------------------------------------------
# Generation of a PDF version of Audiveris handbook
# This file must be run from docs/pdf folder
#---------------------------------------------------------------------------------

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

# curl retrieves HTML content from web site
# grep retrieves all <nav> ... </nav> sections (there are 2 such sections)
# head picks up just the first nav section
# perl removes all <button> ... </button> sections
# sed adjusts href values
curl -sk $PREFIX/audiveris/_pages/handbook/ |\
grep -o -P '<nav .*?</nav>' |\
head -1 |\
perl -pe 's|<button .*?</button>||g' |\
sed -E "s|/audiveris/_pages/|$PREFIX/audiveris/_pages/|g" > build/core.html

# Populate catalog.txt
#---------------------

echo "build/nav.html" > build/catalog.txt

# Retrieve all urls:
# sed(1) injects a line break in front of every URL PREFIX
# sed(2) deletes from each line the " character and everything that follows, leaving the clean URL
# tail deletes the first line, which contains a lonely <nav> tag
sed "s|$PREFIX|\n$PREFIX|g" build/core.html | sed "s/\".*//g" | tail +2 >> build/catalog.txt

# Finalize nav.html
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

"c:/Program Files (x86)/Prince/engine/bin/prince.exe" --input-list=build/catalog.txt -o build/handbook.pdf