import org.gradle.authentication.http.BasicAuthentication

pluginManagement {
    repositories {
        maven {
            name = "JFrog"
            url = uri("https://fbinfra555artifactory.jfrog.io/artifactory/fireblocks-gradle")
            credentials {
                username = System.getenv("JFROG_USERNAME")
                password = System.getenv("JFROG_ACCESS_TOKEN")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }

        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.13.1"
        id("org.jetbrains.kotlin.android") version "2.1.0"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
        id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            name = "JFrog"
            url = uri("https://fbinfra555artifactory.jfrog.io/artifactory/fireblocks-gradle")
            credentials {
                username = System.getenv("JFROG_USERNAME")
                password = System.getenv("JFROG_ACCESS_TOKEN")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }

        // Public repos
        google()
        mavenCentral()
    }
}

rootProject.name = "DynamicSDKExample"

// Note: This project uses pre-built AAR files from the root folder
// No project modules are included - SDK dependencies are AAR files
