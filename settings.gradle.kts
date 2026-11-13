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

rootProject.name = "Homing"
include(":app")
include(":core:designsystem")
include(":core:testing")
include(":feature:onboarding")
include(":feature:home")
include(":feature:connect")
include(":feature:messages")
include(":feature:inbox")
include(":feature:webhooks")
include(":feature:settings")
include(":feature:about")
