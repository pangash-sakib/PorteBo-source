# PorteBo Proguard Rules
-keep class com.sakib.portebo.model.** { *; }
-keepclassmembers class com.sakib.portebo.model.** { *; }
-keep class com.sakib.portebo.message.MessageManager$MessageItem { *; }
-keepclassmembers class com.sakib.portebo.message.MessageManager$MessageItem { *; }
-dontwarn java.lang.invoke.**
