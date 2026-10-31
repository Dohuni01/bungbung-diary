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
        // TODO(Phase 0): Pleos SDK 신청 후 받은 Pleos Maven 저장소 주소를 여기에 추가
        // maven { url = uri("<Pleos Maven URL>") }
    }
}

rootProject.name = "BungbungDiary"
include(":app")
