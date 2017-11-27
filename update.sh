#!/bin/sh

git fetch
git checkout dev

mv ./settings.gradle ./settings.gradle.bk

pathToRepo="https://raw.githubusercontent.com/Nik-Gleb/build-config/master"

rm -f "./android.jar"
rm -f "./build.gradle" && rm -f "./gradle.properties" && rm -f "./version.txt"
rm -rf ./gradle && rm -f "./gradlew" && rm -f "./gradlew.bat"

wget $pathToRepo/version.txt
gradleVersion=$(cat "./version.txt")
rm -f "./version.txt"

#gradleHomeDir=`ls -d $HOME/.gradle/wrapper/dists/gradle-$gradleVersion-all/* | head -n 1`
#gradlePath=$gradleHomeDir/gradle-$gradleVersion/bin
#$gradlePath/gradle --stop
#$gradlePath/gradle wrapper --gradle-version $gradleVersion

wget https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip
unzip gradle-$gradleVersion-bin.zip
rm -f gradle-$gradleVersion-bin.zip
./gradle-$gradleVersion/bin/gradle --stop
./gradle-$gradleVersion/bin/gradle wrapper --gradle-version $gradleVersion
rm -rf ./gradle-$gradleVersion

wget $pathToRepo/build.gradle
wget $pathToRepo/gradle.properties

head -n -1 build.gradle > build.temp ; mv build.temp build.gradle
echo "apply plugin: 'com.android.library'" >> build.gradle
echo "android {" >> build.gradle
echo "  compileSdkVersion Integer.parseInt(sdkVer)" >> build.gradle
echo "  buildToolsVersion buildToolsVer" >> build.gradle
echo "  sourceSets {main {manifest.srcFile './AndroidManifest.xml'}}" >> build.gradle
echo "}" >> build.gradle
echo "<manifest package=\"android\"/>" >> AndroidManifest.xml

./gradlew mockableAndroidJar

head -n -6 build.gradle > build.temp ; mv build.temp build.gradle
echo "task clean {delete rootProject.buildDir}" >> build.gradle
rm -rf ./AndroidManifest.xml

cd ./build/generated
mv `ls | head -n 1` ../../android.jar
cd ../.. && rm -rf ./build

mv ./settings.gradle.bk ./settings.gradle