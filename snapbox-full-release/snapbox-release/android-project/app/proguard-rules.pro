# SnapBox ProGuard Rules

# Keep WebView JavaScript interface
-keepclassmembers class com.snapbox.game.MainActivity$AndroidBridge {
    public *;
}

# Keep all annotations
-keepattributes *Annotation*

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# WebView
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
