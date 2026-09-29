# iTantra ProGuard Rules
# Keep Room entities
-keep class com.itantra.app.data.local.** { *; }

# Keep message models for serialization
-keep class com.itantra.app.domain.model.** { *; }
-keep class com.itantra.app.communication.packet.** { *; }

# Keep enum members
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
