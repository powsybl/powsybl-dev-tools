#!/bin/bash
BUILD_DIR=/tmp/snapshot

CLONE_OPT="--filter=blob:none"
#SET_PROPERTY_OPT="-DgenerateBackupPoms=false -batch-mode --no-transfer-progress"
SET_PROPERTY_OPT="-DgenerateBackupPoms=false -q"
HELP_EVAL_OPT="-q -DforceStdout"

TEST_OPT=""

MVN="mvn -Dmaven.repo.local=$BUILD_DIR/m2_repository"


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
    echo "Usage: $0 <build_from> [OPTIONS]"
    echo "  with <build_from> in:"
    echo "    - \"CORE\":          build all repos;"
    echo "    - \"OLF\":           build OFL, Diagram, Entsoe, Open-RAO, Dynawo and Dependencies;"
    echo "    - \"DIAGRAM\":       build      Diagram, Entsoe, Open-RAO, Dynawo and Dependencies;"
    echo "    - \"ENTSOE\":        build               Entsoe, Open-RAO, Dynawo and Dependencies;"
    echo "    - \"OPENRAO\":       build                       Open-RAO, Dynawo and Dependencies;"
    echo "    - \"DYNAWO\":        build                                 Dynawo and Dependencies;"
    echo "    - \"DEPENDENCIES\":  build                                            Dependencies;"
    echo "    - \"NONE\":          don't build anything (but retrieve the missing repos)"
    echo "  Possible options:"
    echo "    * --skip-tests:      skip tests"
}

function success() {
    echo
    echo
    echo "=== End ==="
    displayInfo
}


# Handle non-positional options
POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    --skip-tests)
    TEST_OPT=$TEST_OPT" -DskipTests"
    echo "-> skipping tests" 
    shift # past argument
    ;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift # past argument
    ;;
esac
done
set -- "${POSITIONAL[@]}" # restore positional parameters


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


SCRIPTS_PATH=$(pwd)/$(dirname "$0")
mkdir -p $BUILD_DIR/.versions
cd $BUILD_DIR

echo
echo "Retrieve missing repositories + set versions in pom.xml files:"

# == powsybl-core ==
echo "- powsybl-core"
if [ ! -e powsybl-core ]; then
  git clone "$CLONE_OPT" https://github.com/powsybl/powsybl-core.git
fi
cd powsybl-core
CORE_VERSION=$($MVN help:evaluate -Dexpression=project.version $HELP_EVAL_OPT)
cd ..


# == powsybl-open-loadflow ==
REPO=powsybl-open-loadflow
echo "- $REPO"
if [ ! -e $REPO ]; then
  SNAPSHOT_BRANCH=$($SCRIPTS_PATH/check_snapshot_branch.sh "https://github.com/powsybl/$REPO.git" "$CORE_VERSION")
  git clone "$CLONE_OPT" -b "$SNAPSHOT_BRANCH" "https://github.com/powsybl/$REPO.git"
fi
cd $REPO
git restore pom.xml
LOADFLOW_VERSION=$($MVN help:evaluate -Dexpression=project.version $HELP_EVAL_OPT)
LOADFLOW_BRANCH=$(git rev-parse --abbrev-ref HEAD)
$MVN versions:set-property -Dproperty=powsybl-core.version -DnewVersion="$CORE_VERSION" $SET_PROPERTY_OPT
cd ..


# == powsybl-diagram ==
REPO=powsybl-diagram
echo "- $REPO"
if [ ! -e $REPO ]; then
  SNAPSHOT_BRANCH=$($SCRIPTS_PATH/check_snapshot_branch.sh "https://github.com/powsybl/$REPO.git" "$CORE_VERSION")
  git clone "$CLONE_OPT" -b "$SNAPSHOT_BRANCH" "https://github.com/powsybl/$REPO.git"
fi
cd $REPO
git restore pom.xml
DIAGRAM_VERSION=$($MVN help:evaluate -Dexpression=project.version $HELP_EVAL_OPT)
DIAGRAM_BRANCH=$(git rev-parse --abbrev-ref HEAD)
$MVN versions:set-property -Dproperty=powsybl-core.version -DnewVersion="$CORE_VERSION" $SET_PROPERTY_OPT
cd ..


# == powsybl-entsoe ==
REPO=powsybl-entsoe
echo "- $REPO"
if [ ! -e $REPO ]; then
  SNAPSHOT_BRANCH=$($SCRIPTS_PATH/check_snapshot_branch.sh "https://github.com/powsybl/$REPO.git" "$CORE_VERSION")
  git clone "$CLONE_OPT" -b "$SNAPSHOT_BRANCH" "https://github.com/powsybl/$REPO.git"
fi
echo -e "\
   powsyblcore.version=$CORE_VERSION\n\
   powsyblopenloadflow.version=$LOADFLOW_VERSION" > .versions/entsoe
cd $REPO
git restore pom.xml
ENTSOE_VERSION=$($MVN help:evaluate -Dexpression=project.version $HELP_EVAL_OPT)
ENTSOE_BRANCH=$(git rev-parse --abbrev-ref HEAD)
$MVN versions:set-property -DpropertiesVersionsFile="../.versions/entsoe" $SET_PROPERTY_OPT
cd ..


# == powsybl-open-rao ==
REPO=powsybl-open-rao
echo "- $REPO"
if [ ! -e $REPO ]; then
  SNAPSHOT_BRANCH=$($SCRIPTS_PATH/check_snapshot_branch.sh "https://github.com/powsybl/$REPO.git" "$CORE_VERSION")
  git clone "$CLONE_OPT" -b "$SNAPSHOT_BRANCH" "https://github.com/powsybl/$REPO.git"
fi
echo -e "\
   powsybl.core.version=$CORE_VERSION\n\
   powsybl.entsoe.version=$ENTSOE_VERSION\n\
   powsybl.openloadflow.version=$LOADFLOW_VERSION" > .versions/open-rao
cd $REPO
git restore pom.xml
OPENRAO_VERSION=$($MVN help:evaluate -Dexpression=project.version $HELP_EVAL_OPT)
OPENRAO_BRANCH=$(git rev-parse --abbrev-ref HEAD)
$MVN versions:set-property -DpropertiesVersionsFile="../.versions/open-rao" $SET_PROPERTY_OPT
cd ..


# == powsybl-dynawo ==
REPO=powsybl-dynawo
echo "- $REPO"
if [ ! -e $REPO ]; then
  SNAPSHOT_BRANCH=$($SCRIPTS_PATH/check_snapshot_branch.sh "https://github.com/powsybl/$REPO.git" "$CORE_VERSION")
  git clone "$CLONE_OPT" -b "$SNAPSHOT_BRANCH" "https://github.com/powsybl/$REPO.git"
fi
cd $REPO
git restore pom.xml
DYNAWO_VERSION=$($MVN help:evaluate -Dexpression=project.version $HELP_EVAL_OPT)
DYNAWO_BRANCH=$(git rev-parse --abbrev-ref HEAD)
$MVN versions:set-property -Dproperty=powsybl-core.version -DnewVersion="$CORE_VERSION" $SET_PROPERTY_OPT
cd ..


# == powsybl-dependencies ==
REPO=powsybl-dependencies
echo "- $REPO"
if [ ! -e $REPO ]; then
  SNAPSHOT_BRANCH=$($SCRIPTS_PATH/check_snapshot_branch.sh "https://github.com/powsybl/$REPO.git" "$CORE_VERSION")
  git clone "$CLONE_OPT" -b "$SNAPSHOT_BRANCH" "https://github.com/powsybl/$REPO.git"
fi
echo -e "\
   powsybl-core.version=$CORE_VERSION\n\
   powsybl-open-loadflow.version=$LOADFLOW_VERSION\n\
   powsybl-diagram.version=$DIAGRAM_VERSION\n\
   powsybl-dynawo.version=$DYNAWO_VERSION\n\
   powsybl-entsoe.version=$ENTSOE_VERSION\n\
   powsybl-open-rao.version=$OPENRAO_VERSION" > .versions/dependencies
cd $REPO
git restore pom.xml
DEPENDENCIES_VERSION=$($MVN help:evaluate -Dexpression=project.version $HELP_EVAL_OPT)
DEPENDENCIES_BRANCH=$(git rev-parse --abbrev-ref HEAD)
$MVN versions:set-property -DpropertiesVersionsFile="../.versions/dependencies" $SET_PROPERTY_OPT
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
  $MVN -batch-mode --no-transfer-progress clean install -DskipTests
  if [[ "$?" -ne 0 ]] ; then
    exit 1
  fi
  cd ..
fi
if [ $START_FROM -lt 2 ]
then
  cd powsybl-open-loadflow
  $MVN -batch-mode --no-transfer-progress clean install $TEST_OPT
  if [[ "$?" -ne 0 ]] ; then
    exit 1
  fi
  cd ..
fi
if [ $START_FROM -lt 3 ]
then
  cd powsybl-diagram
  $MVN -batch-mode --no-transfer-progress clean install $TEST_OPT
  if [[ "$?" -ne 0 ]] ; then
    exit 1
  fi
  cd ..
fi
if [ $START_FROM -lt 4 ]
then
  cd powsybl-entsoe
  $MVN -batch-mode --no-transfer-progress clean install $TEST_OPT
  if [[ "$?" -ne 0 ]] ; then
    exit 1
  fi
  cd ..
fi
if [ $START_FROM -lt 5 ]
then
  cd powsybl-open-rao
  $MVN -batch-mode --no-transfer-progress clean install $TEST_OPT
  if [[ "$?" -ne 0 ]] ; then
    exit 1
  fi
  cd ..
fi
if [ $START_FROM -lt 6 ]
then
  cd powsybl-dynawo
  $MVN -batch-mode --no-transfer-progress clean install $TEST_OPT
  if [[ "$?" -ne 0 ]] ; then
    exit 1
  fi
  cd ..
fi
if [ $START_FROM -lt 7 ]
then
  cd powsybl-dependencies
  $MVN -batch-mode --no-transfer-progress clean install $TEST_OPT
  if [[ "$?" -ne 0 ]] ; then
    exit 1
  fi
  cd ..
fi


success

