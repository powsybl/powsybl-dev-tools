#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <directory> <dictionary.properties>"
    exit -1
fi

path="$1"
dictionary="$2"

function getReportNode() {
  grep -h -A 1 -R "\\.withLocaleMessageTemplate(" --include="*.java" --exclude="*Test*" "$path" | \
     pcregrep -M "\.withLocaleMessageTemplate\((.|\n)*?\)" | \
     awk '{ if (prev) $0 = prev " " $0; if ($0 ~ /[,+]$/) { prev = $0; next } prev = ""; print $0 } END { if (prev) print prev }' | \
     awk -F'"' '{sub(/[ \t]+$/, "", $2); print $2}' | \
     sort -g | uniq > /tmp/found_keys

     sed 's/ =.*$//' $dictionary | uniq > /tmp/dictionary_keys
     diff /tmp/dictionary_keys /tmp/found_keys | grep -E "^>" | cut -c 3-
}

function main() {
  echo -e "\033[35;1;4m=== Missing keys ===\033[0m"
  getReportNode
  echo "==== End ===="
  rm /tmp/found_keys
  rm /tmp/dictionary_keys
}

main
