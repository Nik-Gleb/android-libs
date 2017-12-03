#!/bin/sh

pathToRepo="git@bitbucket.org:NikGleb/android-builds.git"

rm -f "./.android.jar" && rm -f "./.proguard.jar" && rm -f "./.production.jks"
rm -f "./build.gradle" && rm -f "./gradle.properties" && rm -f "./version.txt"
rm -rf ./gradle && rm -f "./gradlew" && rm -f "./gradlew.bat" && rm -f "./settings.gradle"

git clone $pathToRepo
mv android-builds/gradle.txt gradle.txt
mv android-builds/proguard.txt proguard.txt
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

gradleVersion=$(cat "./gradle.txt")
rm -f "./gradle.txt"

gradlePackage=bin
gradleName=gradle-$gradleVersion
gradleFullName=$gradleName-$gradlePackage.zip
gradleDistr=://services.gradle.org/distributions/$gradleFullName
gradleDistrAll=https\://services.gradle.org/distributions/$gradleName-all.zip
wget https$gradleDistr && unzip $gradleFullName && rm -f $gradleFullName
./$gradleName/bin/gradle --stop
./$gradleName/bin/gradle wrapper --gradle-version $gradleVersion
./$gradleName/bin/gradle --stop
rm -rf ./$gradleName

# setup android sdk and licenses
export GRADLE_USER_HOME="~/.gradle"
export ANDROID_HOME="~/.android"
export ANDROID_NDK_HOME="${ANDROID_HOME}/ndk-bundle"
mkdir -p "${ANDROID_HOME}"
if [ ! -f ${ANDROID_HOME}/ndk.zip ]; then wget -O ${ANDROID_HOME}/ndk.zip --quiet https://dl.google.com/android/repository/android-ndk-r13b-linux-x86_64.zip; fi
if [ ! -d ${ANDROID_NDK_HOME} ]; then unzip ${ANDROID_HOME}/ndk.zip -d /tmp/ > /dev/null; mv /tmp/* ${ANDROID_NDK_HOME}; ls ${ANDROID_NDK_HOME}/; fi
mkdir -p "${ANDROID_HOME}/licenses"
echo -e "\n8933bad161af4178b1185d1a37fbf41ea5269c55" > "${ANDROID_HOME}/licenses/android-sdk-license"
echo -e "\n84831b9409646a918e30573bab4c9c91346d8abd" > "${ANDROID_HOME}/licenses/android-sdk-preview-license"
echo -e "\nd975f751698a77b662f1254ddbeed3901e976f5a" > "${ANDROID_HOME}/licenses/intel-android-extra-license"
./gradlew --parallel --stacktrace --no-daemon build 

proguardVersion=$(cat "./proguard.txt")
rm -f "./proguard.txt"
proguardRepo="http://central.maven.org/maven2/net/sf/proguard/proguard-base/$proguardVersion/proguard-base-$proguardVersion.jar"
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
