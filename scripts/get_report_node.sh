#!/bin/bash

path="$1"
function getReportNode() {
  echo
  echo
  grep --color='auto' -R "newReportNode().withMessageTemplate" --exclude="*Test*" --exclude="*Reports.java" "$path"
  echo
}

function main() {
  echo
  echo -e "\033[35;1;4m=== ReportNode calls out of ...Reports.java files ===\033[0m"
  getReportNode
  echo
  echo "==== End ===="

}

main