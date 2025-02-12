#!/bin/bash

path="$1"
function getMessageTemplates() {
  grep -R "\\.withMessageTemplate" --exclude="*Test*" --exclude="*.class" "$path" | awk -F'"' '{sub(/[ \t]+$/, "", $2); sub(/[ \t]+$/, "", $3); print $2, $3, $4}' | sort -g | uniq -u | sed "s/,/=/g" > dictionary.properties
  echo "==== Duplicated lines ===="
  grep -R "\\.withMessageTemplate" --exclude="*Test*" --exclude="*.class" "$path" | awk -F'"' '{sub(/[ \t]+$/, "", $2); sub(/[ \t]+$/, "", $3); print $2, $3, $4}' | sort -g | uniq -d -c | sed "s/,/=/g"
  echo "==== End ===="
  chmod u+x dictionary.properties
}

function main() {
  getMessageTemplates
  echo "INFO : dictionary.properties generated successfully."
}

main