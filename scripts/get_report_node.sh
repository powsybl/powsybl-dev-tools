#!/bin/bash

path="$1"
function getReportNode() {
  grep --color='auto' -R "\\.withMessageTemplate" --include="*.java" --exclude="*Test*" --exclude="*Reports.java" "$path"
}

function main() {
  echo -e "\033[35;1;4m=== ReportNode calls out of ...Reports.java files ===\033[0m"
  getReportNode
  echo "==== End ===="

}

main