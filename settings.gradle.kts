pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GoogleSpaceCleaner"

include(":app")

include(":core:core-ui")
include(":core:core-domain")
include(":core:core-data")
include(":core:core-network")
include(":core:core-security")

include(":feature:auth")
include(":feature:dashboard")
include(":feature:drive-scan")
include(":feature:photos-scan")
include(":feature:gmail-scan")
include(":feature:cleanup")
include(":feature:history")
