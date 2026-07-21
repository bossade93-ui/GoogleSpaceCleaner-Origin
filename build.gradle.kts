// Fichier racine : déclare les plugins utilisés par les sous-modules (via
// `apply false`, chaque module les applique individuellement dans son propre
// build.gradle.kts) ainsi que le scan de vulnérabilités OWASP, appliqué ici
// car il s'exécute une seule fois sur l'ensemble du projet.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.owasp.dependencycheck)
}

dependencyCheck {
    // Scanne les dépendances de tous les modules à la recherche de CVE connues.
    // formats des rapports générés dans build/reports/dependency-check-report.html
    formats = listOf("HTML", "JSON")
    failBuildOnCVSS = 9.0f // bloque le build seulement sur les vulnérabilités critiques
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
