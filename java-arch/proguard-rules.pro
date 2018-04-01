# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/gleb/android-sdk-linux/tools/proguard/proguard-android.txt
# You can update the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class tag to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file tag.
#-renamesourcefileattribute SourceFile

#-dontobfuscate
#-dontoptimize
#-dontshrink

#-verbose
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses

#-optimizations !code/simplification/arithmetic
#-optimizations !code/simplification/cast
-optimizations !code/allocation/variable
#-optimizations !field

-keepparameternames
#-renamesourcefileattribute SourceFile
-keepattributes *Annotation*, Signature
-keepattributes EnclosingMethod
#-keepattributes LineNumberTable, SourceFile
-keepattributes InnerClasses, Exceptions

#
#   Common Android Settings
#
-keepclasseswithmembernames class * {native <methods>;}

-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepnames class * implements android.os.Parcelable {*** CREATOR;}
-keep class * implements android.os.Parcelable {static final android.os.Parcelable$Creator *;}

-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

#
#   Android bug. https://code.google.com/p/android/issues/detail?id=194513
#
-dontnote android.net.http.SslError
-dontnote android.net.http.SslCertificate
-dontnote android.net.http.SslCertificate$DName
-dontnote android.net.http.HttpResponseCache
-dontnote org.apache.http.conn.scheme.HostNameResolver
-dontnote org.apache.http.conn.scheme.SocketFactory
-dontnote org.apache.http.conn.ConnectTimeoutException
-dontnote org.apache.http.params.HttpParams
-dontnote org.apache.http.params.CoreConnectionPNames
-dontnote org.apache.http.params.HttpConnectionParams
-dontnote org.apache.http.conn.scheme.LayeredSocketFactory

-dontnote android.content.pm.ParceledListSlice
-dontnote libcore.icu.ICU
-dontnote android.content.res.ThemedResourceCache
-dontnote android.graphics.Insets
-dontnote android.support.v7.widget.ViewUtils
-dontnote android.support.v4.text.ICUCompatApi23
-dontnote android.support.v4.text.ICUCompatApi21
-dontnote android.support.v4.text.ICUCompatIcs

#-keep class android.support.graphics.drawable.PathParser$PathDataNode {*;}
#-keep class android.support.** {*;}
-keep class org.junit.** {*;}
#-keep class com.android.support.test.** {*;}

-dontnote junit.framework.TestListener
-dontnote junit.framework.AssertionFailedError
-dontnote junit.framework.TestFailure
-dontnote junit.framework.TestResult
-dontnote junit.framework.TestCase
-dontnote junit.framework.Protectable
-dontnote junit.framework.TestSuite
-dontnote junit.framework.Assert
-dontnote junit.framework.Test
-dontnote junit.framework.ComparisonFailure
-dontnote junit.runner.BaseTestRunner
-dontnote junit.runner.Version

-keep public class arch.** {public protected *;}
-keep public interface arch.** {public protected *;}

#-keep public class * {public protected *;}
#-keep public interface * {public protected *;}


-dontwarn android.support.annotation.*

-dontnote android.**
-dontnote com.android.**
-dontnote org.apache.**
-dontnote org.json.**
-dontnote org.xmlpull.**
-dontnote android.json.**
-dontnote android.xmlpull.**
-dontnote dalvik.**

-dontwarn **$$Lambda$*
-dontwarn java.lang.invoke.LambdaMetafactory