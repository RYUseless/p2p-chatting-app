plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: [https://kotlinlang.org/docs/multiplatform-discover-project.html#targets](https://kotlinlang.org/docs/multiplatform-discover-project.html#targets)
    androidLibrary {
        namespace = "ryu.masters_thesis.presentation"
        compileSdk = 36
        minSdk = 24

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // [https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks](https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks)

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // [https://developer.android.com/kotlin/multiplatform/migrate](https://developer.android.com/kotlin/multiplatform/migrate)
    val xcfName = "pressentationKit"

    iosX64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: [https://kotlinlang.org/docs/multiplatform-hierarchy.html](https://kotlinlang.org/docs/multiplatform-hierarchy.html)
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                // Add KMP dependencies here
                implementation("cafe.adriel.voyager:voyager-navigator:1.1.0-beta03")
                implementation("cafe.adriel.voyager:voyager-screenmodel:1.1.0-beta03")
                implementation("cafe.adriel.voyager:voyager-transitions:1.1.0-beta03")
                implementation("cafe.adriel.voyager:voyager-lifecycle-kmp:1.1.0-beta03")
                //ikonky:
                implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
                //call pro ostatni moduly:
                implementation(project(":core"))
                implementation(project(":feature"))
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation("com.journeyapps:zxing-android-embedded:4.3.0")
                implementation("com.google.mlkit:barcode-scanning:17.3.0")
                implementation("androidx.camera:camera-core:1.4.2")
                implementation("androidx.camera:camera-camera2:1.4.2")
                implementation("androidx.camera:camera-lifecycle:1.4.2")
                implementation("androidx.camera:camera-view:1.4.2")
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
                //implementation("androidx.activity:activity-compose:1.10.1")
                //implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.testExt.junit)
            }
        }

        iosMain {
            dependencies {
                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                // part of KMP’s default source set hierarchy. Note that this source set depends
                // on common by default and will correctly pull the iOS artifacts of any
                // KMP dependencies declared in commonMain.
            }
        }
    }

}