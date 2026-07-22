plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.googlespacecleaner.core.data"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

// Room (exportSchema = true, voir AppDatabase.kt) écrit ici un snapshot JSON du
// schéma à chaque version : indispensable pour pouvoir écrire de vraies
// Migration() plus tard au lieu de fallbackToDestructiveMigration(). Ce
// répertoire doit être committé dans le dépôt (ne pas l'ignorer dans .gitignore).
kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation(project(":core:core-domain"))
    implementation(project(":core:core-security"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    implementation(libs.sqlcipher.android)
    implementation(libs.sqlite.ktx)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
