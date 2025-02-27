# ReportNode helper

This folder contains scripts to ease the detection Powsybl `ReportNode` keys in a folder and the generation of a dictionary.

**Requirements:**
You need the `grep`, `pcregrep`, `awk`, `uniq` and `sed` commands available.

## Check missing keys

script: `check_missing_keys.sh`  
use: `check_missing_keys.sh <path to folder> <path to a dictionary>`  
example: `check_missing_keys.sh powsybl-core dictionary.properties`

The script compares the given dictionary keys with the `ReportNode` keys detected within the given folder. Then dictionary missing keys are displayed.

It retrieves the `ReportNode` keys within the folder by scanning its `.java` files except for the filenames matching "Test".

## List java files containing ReportNode

script: `get_report_node.sh`  
use: `get_report_node.sh <path to folder>`  
example: `get_report_node.sh powsybl-core`

As the good practice is to put each repository `ReportNode` definition in one Java class ending by `Reports.java`. The goal here is to list the repository Java files containing `ReportNode` definitions which are not in a `XxxReports.java` file.

It retrieves the `.java` files within the given folder except for the filenames:
- matching "Test"
- ending with "Reports.java"

## Generate dictionary from ReportNode keys

script: `get_message_templates.sh`  
use: `get_message_templates.sh <path to folder>`  
example: `get_message_templates.sh powsybl-core`

The script generates the dictionary file (`dictionary.properties`) with the `key = message template` for each `ReportNode` definition detected within the given folder (excluding the Java test classes).  
It lists the potential errors and duplicates than need to be checked and fixed manually.
