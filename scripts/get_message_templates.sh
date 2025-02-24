#!/bin/bash

path="$1"

function getMessageTemplates() {
  grep -h -A 1 -R "\\.withMessageTemplate" --include="*.java" --exclude="*Test*" "$path" | \
  pcregrep -M "\.withMessageTemplate\((.|\n)*?\)" | \
  awk '{ if (prev) $0 = prev " " $0; if ($0 ~ /[,+]$/) { prev = $0; next } prev = ""; print $0 } END { if (prev) print prev }' | \
  awk -F'"' '{sub(/[ \t]+$/, "", $2); sub(/[ \t]+$/, "", $3); sub(/^[ \t\r\n]+/, "", $4); sub(/^[ \t\r\n]+/, "", $6); print $2, $3, $4, $6}' | \
  sed 's/,/=/' > temp.properties
  echo "==== Duplicated keys ===="
  awk 'NF {print $1}' temp.properties | sort -g | uniq -d -c
  echo
  echo "==== Potential errors - Please check those lines ===="
  grep "\\\\" dictionary.properties
  grep -v "=" dictionary.properties | grep -vE "^   $"
  echo
  echo "==== End ===="
  sort -u -t= -k1,1 temp.properties > dictionary.properties ; rm temp.properties
  chmod u+x dictionary.properties
}

function main() {
  getMessageTemplates
  echo "INFO : dictionary.properties generated successfully."
}

main