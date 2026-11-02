pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Pleos Maven 저장소
        // https://document.pleos.ai/en/docs/pleos-only/getting-started
        maven { url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/") }
    }
}

rootProject.name = "BungbungDiary"
include(":app")
