#!/bin/sh

#git fetch
#git checkout dev

mv ./settings.gradle ./settings.gradle.bk

config_ver="63943c50754263d80704d5e428afee00874e9cc1"
pathToRepo="https://bitbucket.org/NikGleb/android-builds/raw/$config_ver"

rm -f "./.android.jar" && rm -f "./.proguard.jar" && rm -f "./.production.jks"
rm -f "./build.gradle" && rm -f "./gradle.properties" && rm -f "./version.txt"
rm -rf ./gradle && rm -f "./gradlew" && rm -f "./gradlew.bat"

wget $pathToRepo/gradle.txt
gradleVersion=$(cat "./gradle.txt")
rm -f "./gradle.txt"

wget https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip
unzip gradle-$gradleVersion-bin.zip
rm -f gradle-$gradleVersion-bin.zip
./gradle-$gradleVersion/bin/gradle --stop
./gradle-$gradleVersion/bin/gradle wrapper --gradle-version $gradleVersion
rm -rf ./gradle-$gradleVersion

wget $pathToRepo/proguard.txt
proguardVersion=$(cat "./proguard.txt")
rm -f "./proguard.txt"
wget -O .proguard.jar http://central.maven.org/maven2/net/sf/proguard/proguard-base/$proguardVersion/proguard-base-$proguardVersion.jar

wget $pathToRepo/build.gradle
wget $pathToRepo/gradle.properties
wget -O .production.jks $pathToRepo/production.jks

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
mv `ls | head -n 1` ../../.android.jar
cd ../.. && rm -rf ./build

mv ./settings.gradle.bk ./settings.gradle