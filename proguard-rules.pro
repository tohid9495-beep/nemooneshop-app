# Add project specific ProGuard rules here.
-keep public class * extends android.webkit.WebViewClient
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keepattributes *Annotation*
-dontwarn android.webkit.**
