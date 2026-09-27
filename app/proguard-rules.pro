# PdfBox-Android: carrega recursos e fontes por reflexão.
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn com.gemalto.jp2.**

# SDK do Claude (anthropic-java): Jackson serializa os modelos por reflexão.
-keep class com.anthropic.** { *; }
-dontwarn com.anthropic.**
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.hc.**
-dontwarn org.slf4j.**
-dontwarn io.swagger.**
-dontwarn com.github.victools.**
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
