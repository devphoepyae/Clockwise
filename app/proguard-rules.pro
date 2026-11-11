# R8 / resource shrinking is enabled for release builds. Compose, Room, Glance,
# Play Billing and AndroidX ship their own consumer rules; only the generated
# protobuf-lite types need an explicit keep because DataStore serializes them
# through generated code that R8 cannot always see.

-keep class com.pj95.clockwise.data.prefs.SavedCities { *; }
-keep class com.pj95.clockwise.data.prefs.SavedCities$* { *; }
-keep class com.pj95.clockwise.data.prefs.SavedAppTheme { *; }
-keep class com.pj95.clockwise.data.prefs.SavedThemeMode { *; }
-keepclassmembers enum com.pj95.clockwise.data.prefs.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep the generated proto field accessors used by the serializer.
-keepclassmembers class com.pj95.clockwise.data.prefs.** {
    *** getDefaultInstance();
    *** newBuilder();
}
