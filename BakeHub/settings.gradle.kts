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

rootProject.name = "BakeHub"
// NOTE: Firebase requires app/google-services.json to be present (see README) —
// it is intentionally NOT committed/included in this download.
include(":app")
