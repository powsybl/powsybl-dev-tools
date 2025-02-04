#!/bin/bash

function getMessageTemplates() {
  echo
  echo
  grep --color='auto' -R "newReportNode().withMessageTemplate" --exclude="*Test*" --exclude="*Reports.java" /.
  echo
}

function main() {
  echo
}

main