# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in S:\Android\sdk1/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-verbose

-optimizationpasses 5

-keep class com.psymaker.vibraimage.jnilib.VIEngine

-keep public class * extends android.app.Activity
-keep public class * extends android.support.v4.app.FragmentActivity
-keep public class * extends android.support.v4.app.Fragment
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

-keep class com.psymaker.vibraimage.vibram.jni

-keepclassmembers class com.psymaker.vibraimage.vibram.jni{
   public *;
}


-keep class com.psymaker.vibraimage.vibram.pref.PreferenceBool
-keep class com.psymaker.vibraimage.vibram.pref.PreferenceFloat
-keep class com.psymaker.vibraimage.vibram.pref.PreferenceInt
-keep class com.psymaker.vibraimage.vibram.pref.PreferenceList
-keep class com.psymaker.vibraimage.vibram.pref.PreferenceFloat