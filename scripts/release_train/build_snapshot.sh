#!/bin/bash
BUILD_DIR=/tmp/snapshot

function pause() {
    read -s -n 1 -p "Press any key to continue..."
    echo
}

function displayInfo() {
    echo
    echo
    echo "Used versions"
    echo "============="
    echo "Core: $CORE_VERSION (branch main)"
    echo "OLF: $LOADFLOW_VERSION (branch $LOADFLOW_BRANCH)"
    echo "Diagram: $DIAGRAM_VERSION (branch $DIAGRAM_BRANCH)"
    echo "Entsoe: $ENTSOE_VERSION (branch $ENTSOE_BRANCH)"
    echo "Open RAO: $OPENRAO_VERSION (branch $OPENRAO_BRANCH)"
    echo "Dynawo: $DYNAWO_VERSION (branch $DYNAWO_BRANCH)"
    echo "Dependencies: $DEPENDENCIES_VERSION (branch $DEPENDENCIES_BRANCH)"
    echo
    echo
    echo "Maven parameters"
    echo "================"
    grep "<powsybl.*version" ./*/pom.xml | awk -F':|>' '{ gsub(/\.\//,"",$1); gsub(/\/pom\.xml/,"",$1); gsub(/ *</,"",$2); gsub(/<\/.*/,"",$3); printf("%-25s %-30s %s \n",  $1, $2, $3) }' | sort
    echo
}

function usage() {
    echo "Usage: $0 <build_from>"
    echo "  with <build_from> in:"
    echo "    - \"CORE\":          build all repos;"
    echo "    - \"OLF\":           build OFL, Diagram, Entsoe, Open-RAO, Dynawo and Dependencies;"
    echo "    - \"DIAGRAM\":       build      Diagram, Entsoe, Open-RAO, Dynawo and Dependencies;"
    echo "    - \"ENTSOE\":        build               Entsoe, Open-RAO, Dynawo and Dependencies;"
    echo "    - \"OPENRAO\":       build                       Open-RAO, Dynawo and Dependencies;"
    echo "    - \"DYNAWO\":        build                                 Dynawo and Dependencies;"
    echo "    - \"DEPENDENCIES\":  build                                            Dependencies;"
    echo "    - \"NONE\":          don't build anything (but retrieve the missing repos)"
}

function success() {
    echo
    echo
    echo "=== End ==="
    displayInfo
}


if [ "$#" -ne 1 ]; then
    usage
    exit 3
fi

case $1 in

  CORE)
    START_FROM=0
    ;;

  OLF)
    START_FROM=1
    ;;

  DIAGRAM)
    START_FROM=2
    ;;

  ENTSOE)
    START_FROM=3
    ;;

  OPENRAO)
    START_FROM=4
    ;;

  DYNAWO)
    START_FROM=5
    ;;

  DEPENDENCIES)
    START_FROM=6
    ;;

  NONE)
    START_FROM=99
    ;;

  *)
    echo "Invalid value: $1"
    usage
    exit 2;
    ;;
esac


# Build starting from:
# - 0: Core
# - 1: OLF
# - 2: Diagram
# - 3: Entsoe
# - 4: Open-RAO
# - 5: Dynawo
# - 6: Dependencies
# - Greater: Won't build anything
#START_FROM=99
#START_FROM=0



CLONE_OPT="--filter=blob:none"
#SET_PROPERTY_OPT="-DgenerateBackupPoms=false -batch-mode --no-transfer-progress"
SET_PROPERTY_OPT="-DgenerateBackupPoms=false -q"

SCRIPTS_PATH=$(pwd)
mkdir -p $BUILD_DIR
cd $BUILD_DIR

echo
echo "Retrieve missing repositories + set versions in pom.xml files:"

# == powsybl-core ==
echo "- powsybl-core"
if [ ! -e powsybl-core ]; then
  git clone "$CLONE_OPT" https://github.com/powsybl/powsybl-core.git
fi
cd powsybl-core
CORE_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
cd ..


# == powsybl-open-loadflow ==
REPO=powsybl-open-loadflow
echo "- $REPO"
if [ ! -e $REPO ]; then
  SNAPSHOT_BRANCH=$($SCRIPTS_PATH/check_snapshot_branch.sh "https://github.com/powsybl/$REPO.git" "$CORE_VERSION")
  git clone "$CLONE_OPT" -b "$SNAPSHOT_BRANCH" "https://github.com/powsybl/$REPO.git"
fi
cd $REPO
mvn versions:set-property -Dproperty=powsybl-core.version -DnewVersion="$CORE_VERSION" $SET_PROPERTY_OPT
LOADFLOW_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
LOADFLOW_BRANCH=$(git rev-parse --abbrev-ref HEAD)
cd ..


# == powsybl-diagram ==
REPO=powsybl-diagram
echo "- $REPO"
if [ ! -e $REPO ]; then
  SNAPSHOT_BRANCH=$($SCRIPTS_PATH/check_snapshot_branch.sh "https://github.com/powsybl/$REPO.git" "$CORE_VERSION")
  git clone "$CLONE_OPT" -b "$SNAPSHOT_BRANCH" "https://github.com/powsybl/$REPO.git"
fi
cd $REPO
mvn versions:set-property -Dproperty=powsybl-core.version -DnewVersion="$CORE_VERSION" $SET_PROPERTY_OPT
DIAGRAM_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
DIAGRAM_BRANCH=$(git rev-parse --abbrev-ref HEAD)
cd ..


# == powsybl-entsoe ==
REPO=powsybl-entsoe
echo "- $REPO"
if [ ! -e $REPO ]; then
  SNAPSHOT_BRANCH=$($SCRIPTS_PATH/check_snapshot_branch.sh "https://github.com/powsybl/$REPO.git" "$CORE_VERSION")
  git clone "$CLONE_OPT" -b "$SNAPSHOT_BRANCH" "https://github.com/powsybl/$REPO.git"
fi
cd $REPO
mvn versions:set-property -Dproperty=powsyblcore.version -DnewVersion="$CORE_VERSION" $SET_PROPERTY_OPT
mvn versions:set-property -Dproperty=powsyblopenloadflow.version -DnewVersion="$LOADFLOW_VERSION" $SET_PROPERTY_OPT
ENTSOE_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
ENTSOE_BRANCH=$(git rev-parse --abbrev-ref HEAD)
cd ..


# == powsybl-open-rao ==
REPO=powsybl-open-rao
echo "- $REPO"
if [ ! -e $REPO ]; then
  SNAPSHOT_BRANCH=$($SCRIPTS_PATH/check_snapshot_branch.sh "https://github.com/powsybl/$REPO.git" "$CORE_VERSION")
  git clone "$CLONE_OPT" -b "$SNAPSHOT_BRANCH" "https://github.com/powsybl/$REPO.git"
fi
cd $REPO
mvn versions:set-property -Dproperty=powsybl.core.version -DnewVersion="$CORE_VERSION" $SET_PROPERTY_OPT
mvn versions:set-property -Dproperty=powsybl.entsoe.version -DnewVersion="$ENTSOE_VERSION" $SET_PROPERTY_OPT
mvn versions:set-property -Dproperty=powsybl.openloadflow.version -DnewVersion="$LOADFLOW_VERSION" $SET_PROPERTY_OPT
OPENRAO_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
OPENRAO_BRANCH=$(git rev-parse --abbrev-ref HEAD)
cd ..


# == powsybl-dynawo ==
REPO=powsybl-dynawo
echo "- $REPO"
if [ ! -e $REPO ]; then
  SNAPSHOT_BRANCH=$($SCRIPTS_PATH/check_snapshot_branch.sh "https://github.com/powsybl/$REPO.git" "$CORE_VERSION")
  git clone "$CLONE_OPT" -b "$SNAPSHOT_BRANCH" "https://github.com/powsybl/$REPO.git"
fi
cd $REPO
mvn versions:set-property -Dproperty=powsybl-core.version -DnewVersion="$CORE_VERSION" $SET_PROPERTY_OPT
DYNAWO_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
DYNAWO_BRANCH=$(git rev-parse --abbrev-ref HEAD)
cd ..


# == powsybl-dependencies ==
REPO=powsybl-dependencies
echo "- $REPO"
if [ ! -e $REPO ]; then
  SNAPSHOT_BRANCH=$($SCRIPTS_PATH/check_snapshot_branch.sh "https://github.com/powsybl/$REPO.git" "$CORE_VERSION")
  git clone "$CLONE_OPT" -b "$SNAPSHOT_BRANCH" "https://github.com/powsybl/$REPO.git"
fi
cd $REPO
mvn versions:set-property -Dproperty=powsybl-core.version -DnewVersion="$CORE_VERSION" $SET_PROPERTY_OPT
mvn versions:set-property -Dproperty=powsybl-open-loadflow.version -DnewVersion="$LOADFLOW_VERSION" $SET_PROPERTY_OPT
mvn versions:set-property -Dproperty=powsybl-diagram.version -DnewVersion="$DIAGRAM_VERSION" $SET_PROPERTY_OPT
mvn versions:set-property -Dproperty=powsybl-dynawo.version -DnewVersion="$DYNAWO_VERSION" $SET_PROPERTY_OPT
mvn versions:set-property -Dproperty=powsybl-entsoe.version -DnewVersion="$ENTSOE_VERSION" $SET_PROPERTY_OPT
mvn versions:set-property -Dproperty=powsybl-open-rao.version -DnewVersion="$OPENRAO_VERSION" $SET_PROPERTY_OPT
DEPENDENCIES_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
DEPENDENCIES_BRANCH=$(git rev-parse --abbrev-ref HEAD)
cd ..


if [ $START_FROM -eq 99 ]
then
    success
    exit 0
fi

displayInfo
pause

# == BUILD ==
if [ $START_FROM -lt 1 ]
then
  cd powsybl-core
  mvn -batch-mode --no-transfer-progress clean install -DskipTests
  if [[ "$?" -ne 0 ]] ; then
    exit 1
  fi
  cd ..
fi
if [ $START_FROM -lt 2 ]
then
  cd powsybl-open-loadflow
  mvn -batch-mode --no-transfer-progress clean install
  if [[ "$?" -ne 0 ]] ; then
    exit 1
  fi
  cd ..
fi
if [ $START_FROM -lt 3 ]
then
  cd powsybl-diagram
  mvn -batch-mode --no-transfer-progress clean install
  if [[ "$?" -ne 0 ]] ; then
    exit 1
  fi
  cd ..
fi
if [ $START_FROM -lt 4 ]
then
  cd powsybl-entsoe
  mvn -batch-mode --no-transfer-progress clean install
  if [[ "$?" -ne 0 ]] ; then
    exit 1
  fi
  cd ..
fi
if [ $START_FROM -lt 5 ]
then
  cd powsybl-open-rao
  mvn -batch-mode --no-transfer-progress clean install
  if [[ "$?" -ne 0 ]] ; then
    exit 1
  fi
  cd ..
fi
if [ $START_FROM -lt 6 ]
then
  cd powsybl-dynawo
  mvn -batch-mode --no-transfer-progress clean install
  if [[ "$?" -ne 0 ]] ; then
    exit 1
  fi
  cd ..
fi
if [ $START_FROM -lt 7 ]
then
  cd powsybl-dependencies
  mvn -batch-mode --no-transfer-progress clean install
  if [[ "$?" -ne 0 ]] ; then
    exit 1
  fi
  cd ..
fi


success

