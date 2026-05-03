#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Maven Start Up Batch script
# Required ENV vars:
# ------------------
#   JAVA_HOME - location of a JDK home dir
# Optional ENV vars
# -----------------
#   MAVEN_OPTS - parameters passed to the Java VM when running Maven
# ----------------------------------------------------------------------------

if [ -z "$JAVA_HOME" ]; then
    echo "Error: JAVA_HOME is not set." >&2
    exit 1
fi

MAVEN_PROJECTBASEDIR="${BASH_SOURCE%/*}"
if [ -z "$MAVEN_PROJECTBASEDIR" ]; then
    MAVEN_PROJECTBASEDIR="."
fi
MAVEN_PROJECTBASEDIR="$(cd "$MAVEN_PROJECTBASEDIR" && pwd)"

MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"
DISTRIBUTION_URL=$(grep "distributionUrl" "$MAVEN_WRAPPER_PROPERTIES" | sed -e 's/distributionUrl=//')

MAVEN_USER_HOME="${MAVEN_USER_HOME:-$HOME/.m2}"
MAVEN_WRAPPER_HOME="$MAVEN_USER_HOME/wrapper"
# ZIP name includes -bin suffix; extracted directory does not
DISTRIBUTION_ZIP_NAME=$(basename "$DISTRIBUTION_URL" .zip)
DISTRIBUTION_NAME=$(echo "$DISTRIBUTION_ZIP_NAME" | sed -e 's/-bin$//')
MAVEN_HOME="$MAVEN_WRAPPER_HOME/$DISTRIBUTION_NAME"

if [ ! -d "$MAVEN_HOME" ]; then
    mkdir -p "$MAVEN_WRAPPER_HOME"
    echo "Downloading Maven from $DISTRIBUTION_URL"
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL -o "$MAVEN_WRAPPER_HOME/$DISTRIBUTION_NAME.zip" "$DISTRIBUTION_URL"
    elif command -v wget >/dev/null 2>&1; then
        wget -q -O "$MAVEN_WRAPPER_HOME/$DISTRIBUTION_NAME.zip" "$DISTRIBUTION_URL"
    else
        echo "Error: Neither curl nor wget is available." >&2
        exit 1
    fi
    unzip -q "$MAVEN_WRAPPER_HOME/$DISTRIBUTION_NAME.zip" -d "$MAVEN_WRAPPER_HOME"
    rm -f "$MAVEN_WRAPPER_HOME/$DISTRIBUTION_NAME.zip"
fi

MAVEN_CMD="$MAVEN_HOME/bin/mvn"
exec "$MAVEN_CMD" "$@"
