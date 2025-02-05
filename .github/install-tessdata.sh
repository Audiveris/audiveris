#!/bin/sh
set -e

# This script loads tesseract-ocr files from github

verify_single_argument_given() {
  if [ $# -eq 0 ]; then
    echo "No arguments supplied. Run e.g. 'install-tessdata.sh eng'. Aborting."
    exit 1
  fi
  if [ $# -gt 1 ]; then
    echo "Too many arguments supplied. Run e.g. 'install-tessdata.sh eng'. Aborting."
    exit 1
  fi
}

verify_tessdata_prefix_set() {
  if [ -z "${TESSDATA_PREFIX}" ]; then
    echo "Environment variable TESSDATA_PREFIX is not set. Aborting."
    exit 2
  fi
}

verify_tessdata_tag_set() {
  if [ -z "${TESSDATA_TAG}" ]; then
    echo "Environment variable TESSDATA_TAG is not set. Please set it to a valid github tag of tesseract-ocr/tessdata. Aborting."
    exit 2
  fi
}

install_tessdata_language() {
  language=$1
  echo "Loading tessdata language $language..."
  wget --no-verbose -P "$TESSDATA_PREFIX" "https://github.com/tesseract-ocr/tessdata/raw/${TESSDATA_TAG}/${language}.traineddata"
}

verify_single_argument_given "$@"
verify_tessdata_prefix_set
verify_tessdata_tag_set
install_tessdata_language "$1"
