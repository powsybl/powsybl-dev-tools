#!/bin/bash

path="$1"
function getMessageTemplates() {
  echo
  echo
  grep --color='auto' -R "newReportNode().withMessageTemplate" "$path" | awk -F '"' '{print $2, $3, $4}' | sort -g | uniq | sed "s/,/=/g" > message_templates.txt
  echo
}

function main() {
  echo
  getMessageTemplates
  echo "INFO : message_templates.txt generated successfully"
}

main