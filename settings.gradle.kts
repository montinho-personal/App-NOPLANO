pluginManagement {
    repositories {
        google()
        // Espelho oficial do Maven Central mantido pelo Google (evita limite de downloads).
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Espelho oficial do Maven Central mantido pelo Google (evita limite de downloads).
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
}
rootProject.name = "NoPlano"
include(":app")
