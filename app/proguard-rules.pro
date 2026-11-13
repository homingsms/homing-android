-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,Exceptions
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
-renamesourcefileattribute SourceFile

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers @kotlinx.serialization.Serializable class com.homingsms.app.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers @kotlinx.serialization.Serializable class com.homingsms.core.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers enum com.homingsms.core.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}
-keepclassmembers enum com.homingsms.app.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.TypeConverters class * { *; }
-keepclassmembers class * { @androidx.room.TypeConverter *; }

-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

-keep class org.koin.core.** { *; }

-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.eclipse.jetty.**
-dontwarn reactor.blockhound.**
-dontwarn com.aayushatharva.brotli4j.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.jcraft.jzlib.**
-dontwarn com.ning.compress.**
-dontwarn com.oracle.svm.core.annotate.**
-dontwarn lzma.sdk.**
-dontwarn net.jpountz.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.jboss.marshalling.**
-dontwarn sun.security.x509.**
-dontwarn java.lang.management.**
-dontwarn javax.naming.**

-dontwarn com.google.common.**

-keep class com.homingsms.app.modules.mms.pdu.** { *; }

-keep class com.google.i18n.phonenumbers.** { *; }
