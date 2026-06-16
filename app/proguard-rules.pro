# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

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
# Kotlin serialization / import DTOs used for portable bundle parsing.
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,Signature,InnerClasses,EnclosingMethod
-keep class com.ahmedyejam.mks.data.importer.dto.** { *; }
-keepclassmembers class com.ahmedyejam.mks.data.importer.dto.** { *; }

# Room and Moshi publish consumer rules, but keep generated adapter metadata stable.
-keep class **JsonAdapter { *; }
-keep class **JsonAdapterKt { *; }

# Apache POI may reference optional XML/security backends depending on workbook contents.
-dontwarn org.apache.xmlbeans.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.commons.compress.**

# Fix for missing classes in log4j, findbugs, and poi
-dontwarn aQute.bnd.annotation.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
-dontwarn org.osgi.framework.**
