#!/bin/sh
echo "Release $1($(date -u +"%H:%M:%S %d.%m.%Y <%Z>")): $2" >> CHANGELOG
git add --all && git commit -m "Release $1. $2" && git push
git checkout master && git fetch && git merge dev && git push
./gradlew clean && ./gradlew assembleRelease -PVERSION_NAME=$1
./gradlew publish -PVERSION_NAME=$1
git checkout dev
