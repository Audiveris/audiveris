#! /bin/bash
trap 'echo "ERROR in $BASH_COMMAND" >&2' ERR

TAGNAME=4.1.0
PROJECT=tessdata

get_languages() {
    echo "Fetching language list for $PROJECT $TAGNAME ..." >&2
    TAG=$(curl -s  "$TESSDATA/tags" | jq -r '.[] | select(.name=="'"$TAGNAME"'").commit.sha')
    [[ $TAG ]]

    mapfile -t LANG \
	< <(curl -s "$TESSDATA/git/trees/$TAG" | \
		jq -r '.tree[] | select (.path|endswith(".traineddata")) |  [ .path,.sha | rtrimstr(".traineddata") ] | join(":")')
    mapfile -t CURR < <(sed -nE '/url:|path:/s,.*/([a-z_]+)\.traineddata,\1,p' lang_sources.yml)
}

print_languages() {
    echo "=== Available languages ==="

    for i in "${!LANG[@]}"; do
	if [[ $((i % 5)) == 4 ]]; then
	    printf "$i: ${LANG[$i]%:*}\n"
	else
	    printf "$i: ${LANG[$i]%:*}\t\t"
	fi
    done
    printf "\n"
    echo "=== Currently configured languages ==="
    for i in "${!CURR[@]}"; do
	if [[ $((i % 5)) == 4 ]]; then
	    printf "$i: ${CURR[$i]%:*}\n"
	else
	    printf "$i: ${CURR[$i]%:*}\t\t"
	fi
    done
}

interactive() {
    local ans

    while true; do
	echo -n "enter number or abbrev to download, ENTER to end, ? to list languages: " >&2
	read ans
	case $ans in
	    "") break;;
	    "?") print_languages; continue;;
	esac
	lang=$(find_language "$ans")
	[[ $lang ]] || continue
	LANGUAGES=("${LANGUAGES[@]}" "$lang")
    done
}

find_language() {
    local lang=$1

    if [[ ! $lang =~ '^[0-9]+$' ]]; then
	local i=0
	while [[ $((i++)) -lt ${#LANG[@]} ]]; do
	    case ${LANG[$i]} in
		$lang:*)
		    lang=$i
		    break;;
	    esac
	done
    fi
    if [[ $lang -lt ${#LANG[@]} ]]; then
	echo "${LANG[$lang]}"
	echo "${LANG[$lang]} selected" >&2
    else
	echo "language $lang not found" >&2
    fi
}

declare -a LANGUAGES
CHECK=
INTERACTIVE=
LIST=

while [[ $# -gt 0 ]]; do
    echo "$1" >&2
    case $1 in
	-\?|--help)
	    echo "Usage:
$0 [-b|--best] [-c|--check] language [language ...]
$0 [-b|--best] [-c|--check] (-i|--interactive)
$0 [-b|--best] [-l|--list]
$0 -?" >&2
	    exit 0
	    ;;
	-l|--list)
	    LIST=yes
	    ;;
	-c|--check)
	    CHECK=yes
	    ;;
	-b|--best)
	    PROJECT=tessdata_best
	    ;;
	-i|--interactive)
	    INTERACTIVE=yes
	    ;;
	*)
	    break
	    ;;
    esac
    shift
done

TESSDATA=https://api.github.com/repos/tesseract-ocr/$PROJECT
TESSDL=https://github.com/tesseract-ocr/$PROJECT/raw/$TAGNAME

if [[ $LIST ]]; then
    get_languages
    print_languages
    exit 0
fi

LANGUAGES=($@)
get_languages
if [[ $INTERACTIVE ]]; then
    interactive
fi

mkdir -p languages

lang_files=""
for lang in "${LANGUAGES[@]}"; do
    case $lang in
	*:*) ;;
	*) lang=$(find_language "$lang");;
    esac
    [[ $lang ]] || continue
    hash=${lang#*:}
    name=${lang%:*}
    file=languages/"$name.traineddata"
    if [[ -f "$file" ]]; then
	echo "$file exists already, skipping" >&2
	continue
    fi
    echo "Fetching language data for $name ($TAGNAME) ..." >&2
    rm -f "$file"
    curl "$TESSDATA/git/blobs/$hash" | jq -r '.content' | \
	base64 -d >"$file"
    [[ -f "$file" ]]
    check=$(cat <(printf 'blob %d\0' "$(stat -c %s "$file")") "$file" | \
		sha1sum | cut -d " " -f 1)
    if [[ $check != $hash ]] ; then
	echo "sha1 hash of $file doesn't match expected $hash" >&2
	rm -f "$file"
	continue
    fi
    check=$(sha1sum "$file" | cut -d " " -f 1)
    if [[ $CHECK = yes ]]; then
	# test if "simple" download yields the correct SHA1
	curl -L -o "${file}-1" "$TESSDL/$name.traineddata"
	check1=$(sha1sum "${file}-1" | cut -d " " -f 1)
    else
	check1=$check
    fi
    if [[ $check = $check1 ]]; then
	echo "URL can be used for $lang" >&2
	rm -fv "$file" "${file}-1"
	lang_files="$lang_files
      - type: file
        url: $TESSDL/$name.traineddata
        sha1: $check"
    else
	echo "URL can't be used for $lang" >&2
	rm -fv "$file"
	lang_files="$lang_files
      - type: file
        path: $file
        sha1: $hash"
    fi
done

if [[ $lang_files ]]; then
    echo "$lang_files" >>lang_sources.yml
    echo "lang_sources.yml generated" >&2
fi
