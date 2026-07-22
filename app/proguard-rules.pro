# Règles R8/ProGuard — sans ces règles, le build release planterait au
# runtime car R8 supprimerait par défaut les éléments utilisés par réflexion.

# --- Gson (sérialisation des DTO réseau) ---
# Conserve les champs des DTO Retrofit/Gson : sans ceci, la désérialisation
# retourne des objets avec des champs null même si l'API répond correctement.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.googlespacecleaner.core.network.**.*Dto { *; }
-keep class com.googlespacecleaner.core.network.**.*Response { *; }
-keep class com.googlespacecleaner.core.network.**.*Request { *; }
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# --- Retrofit ---
-keepattributes Exceptions
-keep,allowobfuscation interface retrofit2.**
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# --- Room ---
# Room génère du code à la compilation (KAPT) ; les entités doivent
# conserver leurs champs pour que le mapping SQL fonctionne après minification.
-keep class com.googlespacecleaner.core.data.local.db.*Entity { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# --- Hilt / Dagger ---
# Généralement pas nécessaire (Hilt fournit ses propres règles via son
# artefact), mais on garde les classes générées par explicité et robustesse.
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager
-keepclasseswithmembers class * {
    @dagger.hilt.android.AndroidEntryPoint <methods>;
}

# --- SQLCipher (bibliothèque native) ---
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**

# --- Modèles de domaine (core-domain) ---
# Sérialisés/désérialisés via les mappers Room, jamais via Gson directement,
# mais on les garde par prudence car ils circulent entre de nombreuses couches.
-keep class com.googlespacecleaner.core.domain.model.** { *; }

# --- Google Sign-In / Play Services ---
-keep class com.google.android.gms.auth.api.signin.** { *; }
-dontwarn com.google.android.gms.**

# --- Kotlin coroutines / Flow (évite des avertissements bruyants, pas des crashs) ---
-dontwarn kotlinx.coroutines.**
