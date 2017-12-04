#!/bin/sh

pathToRepo="git@bitbucket.org:NikGleb/android-builds.git"

rm -f "./.android.jar" && rm -f "./.proguard.jar" && rm -f "./.production.jks"
rm -f "./build.gradle" && rm -f "./gradle.properties"
rm -rf ./gradle && rm -f "./gradlew" && rm -f "./gradlew.bat" && rm -f "./settings.gradle"

git clone $pathToRepo
mv android-builds/build.gradle build.gradle
mv android-builds/gradle.properties gradle.properties
mv android-builds/production.jks .production.jks

if [ -d ".git/hooks" ]; then
  rm -rf .git/hooks/commit-msg
  mv android-builds/commit-msg .git/hooks/commit-msg
  chmod +x .git/hooks/commit-msg
fi

#mv android-builds/update.sh ./update.sh
#mv android-builds/release.sh ./release.sh

rm -rf android-builds

while IFS='=' read -r key value
do
  key=$(echo $key | tr '.' '_')
  eval "${key}='${value}'"
done < "./gradle.properties"

if [ -n "$PLATFORM_API" ]; then
  platformApi=$PLATFORM_API
else
  platformApi=$sdkVer
fi

$ANDROID_HOME/tools/bin/sdkmanager --update && yes | $ANDROID_HOME/tools/bin/sdkmanager --licenses
$ANDROID_HOME/tools/bin/sdkmanager \
  "tools" \
  "platforms;android-$platformApi" \
  "platform-tools" \
  "patcher;v4" \
  "ndk-bundle" \
  "extras;google;google_play_services" \
  "extras;google;m2repository" \
  "extras;android;m2repository" \
  "cmake;$cmakeVer" \
  "build-tools;$buildToolsVer" 

gradlePackage=bin
gradleName=gradle-$gradle
gradleFullName=$gradleName-$gradlePackage.zip
gradleDistr=://services.gradle.org/distributions/$gradleFullName
gradleDistrAll=https\://services.gradle.org/distributions/$gradleName-all.zip
wget https$gradleDistr && unzip $gradleFullName && rm -f $gradleFullName
./$gradleName/bin/gradle --stop
./$gradleName/bin/gradle wrapper --gradle-version $gradle
./$gradleName/bin/gradle --stop
rm -rf ./$gradleName

proguardRepo="http://central.maven.org/maven2/net/sf/proguard/proguard-gradle/$proguard/proguard-gradle-$proguard.jar"
wget -O .proguard.jar $proguardRepo

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

wrapProp=./gradle/wrapper/gradle-wrapper.properties
head -n -1 $wrapProp > $wrapProp.temp ; mv $wrapProp.temp $wrapProp
echo "distributionUrl=$gradleDistrAll" >> $wrapProp

for i in * ; do
  if [ -d "$i" ]; then
    if [ -f $i/build.gradle ]; then
      echo "include ':$i'" >> settings.gradle
    fi
  fi
done
