#!/bin/sh -l
set -e

echo "======== Building and testing agent ========="
cd agent
sh -c "mvn ${MAVENPARAMS} verify"
sh -c "mvn -P SapAgent -DfullTest=true ${MAVENPARAMS} clean verify"
echo "======== Finished ==========================="
