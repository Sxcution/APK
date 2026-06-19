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

# Keep Logger and remove debug logs in release
-assumenosideeffects class com.sxcution.app.core.Logger {
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Remove Android Log calls in release builds.
# This ensures no debug logs leak location information.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Keep service classes
-keep class com.sxcution.app.service.ForegroundLocationService { *; }
-keep class com.sxcution.app.services.LocationService { *; }

# Keep core classes
-keep class com.sxcution.app.core.LocationBus { *; }
-keep class com.sxcution.app.core.Logger { *; }

# Keep UI classes
-keep class com.sxcution.app.ui.Toaster { *; }

# Keep notification utils
-keep class com.sxcution.app.utils.NotificationUtils { *; }

# Keep data classes
-keep class com.sxcution.app.core.LocationBus$LocationUpdate { *; }

# Keep dialog classes
-keep class com.sxcution.app.dialogs.SavedPlacesDialogFragment { *; }
-keep class com.sxcution.app.dialogs.SavedPlacesDialogFragment$OnPlaceSelectedListener { *; }
-keep class com.sxcution.app.dialogs.SettingsDialogFragment { *; }
-keep class com.sxcution.app.dialogs.SettingsDialogFragment$OnSettingsChangedListener { *; }

# Keep repository classes
-keep class com.sxcution.app.repository.SavedPlacesRepository { *; }
-keep class com.sxcution.app.repository.SimpleSavedPlacesRepository { *; }

# Keep adapter classes
-keep class com.sxcution.app.adapters.SavedPlacesSimpleAdapter { *; }
-keep class com.sxcution.app.adapters.SavedPlacesAdapter { *; }

# Keep data model classes
-keep class com.sxcution.app.data.SavedPlace { *; }
-keep class com.sxcution.app.data.SimpleSavedPlace { *; }
-keep class com.sxcution.app.models.PlaceMeta { *; }

# Keep utility classes
-keep class com.sxcution.app.utils.GeocodingUtils { *; }
-keep class com.sxcution.app.utils.Naming { *; }

# Keep receiver classes
-keep class com.sxcution.app.receivers.LocationReceiver { *; }

# Keep SeekBar and related classes
-keep class android.widget.SeekBar { *; }
-keep class android.widget.SeekBar$OnSeekBarChangeListener { *; }

# Keep all View classes to prevent crashes
-keep class android.view.View { *; }
-keep class android.widget.* { *; }

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Google Maps
-keep class com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.gms.maps.**

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
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

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep R class
-keep class com.sxcution.app.R$* { *; }

# Keep BuildConfig
-keep class com.sxcution.app.BuildConfig { *; }

# Keep all classes in main package
-keep class com.sxcution.app.** { *; }

# Keep Dexter permission library
-keep class com.karumi.dexter.** { *; }
-dontwarn com.karumi.dexter.**

# Keep coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep Gson
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Keep geohash library
-keep class ch.hsr.geohash.** { *; }
-dontwarn ch.hsr.geohash.**
