#!/bin/bash

path="$1"
function getMessageTemplates() {
  grep -R "\\.withMessageTemplate" --exclude="*Test*" --include="*.java" "$path" | awk -F'"' '{sub(/[ \t]+$/, "", $2); sub(/[ \t]+$/, "", $3); print $2, $3, $4}' | sort -g | sed 's/,/=/' > temp.properties
  echo "==== Duplicated keys ===="
  awk 'NF {print $1}' temp.properties | sort -g | uniq -d -c
  echo "==== End ===="
  sort -u -t= -k1,1 temp.properties > dictionary.properties ; rm temp.properties
  chmod u+x dictionary.properties
}

function main() {
  getMessageTemplates
  echo "INFO : dictionary.properties generated successfully."
}

main