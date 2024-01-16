pluginManagement {
    repositories {
        google()
        mavenCentral{
            url = java.net.URI("http://download.iypack.ir/repository/MyPack/")
            isAllowInsecureProtocol = true // add this line
        }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral{
            url = java.net.URI("http://download.iypack.ir/repository/MyPack/")
            isAllowInsecureProtocol = true // add this line
        }
    }
}

rootProject.name = "NotificationMaster"
include(":app")
