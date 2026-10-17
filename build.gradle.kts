plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

group = "org.connecttag.libs.kotlin"
version = "1.0.6"

android {
    namespace = "org.connecttag.lib.update"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Google Play App Update Library
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Testing
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "org.connecttag.libs.kotlin"
                artifactId = "update-play-lib"
                version = "1.0.6"

                pom {
                    name.set("ConnectTag Update Play Lib")
                    description.set("Google Play In-App Updates abstraction with flexible and immediate update flows, reactive state listeners, and fake testing engine")
                    url.set("https://github.com/connecttag/update-play-lib")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("connecttagye")
                            name.set("Connect Tag")
                            email.set("connecttagye@gmail.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:https://github.com/connecttag/update-play-lib.git")
                        developerConnection.set("scm:git:ssh://git@github.com:connecttag/update-play-lib.git")
                        url.set("https://github.com/connecttag/update-play-lib/tree/main")
                    }
                }
            }
        }
    }
}
