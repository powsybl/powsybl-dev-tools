#!/bin/bash

repo=$1
core_version=$2

SNAPSHOT_BRANCH=$(git ls-remote --heads "$repo" | grep -E "refs/heads/ci/core-$(echo $core_version | grep -q SNAPSHOT && echo "$core_version" || echo "$core_version-SNAPSHOT")" | sed 's/.*refs\/heads\///')
if [ -n "$SNAPSHOT_BRANCH" ]; then
    # SNAPSHOT version exists
    echo "$SNAPSHOT_BRANCH"
else
    # No SNAPSHOT branch found
    echo "main"
fi

