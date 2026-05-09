plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {

    androidLibrary {
        namespace = "ryu.masters_thesis.data"
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

    val xcfName = "dataKit"

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

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite.bundled)
                //di sranec, aka pro di .kt
                implementation("androidx.datastore:datastore-preferences-core:1.1.1")
                // commonMain.dependencies:
                implementation("io.insert-koin:koin-core:4.0.0")
                implementation("androidx.datastore:datastore-preferences-core:1.1.1")
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.sqlcipher.android)
                implementation("io.insert-koin:koin-android:4.0.0")
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
            }
        }
    }

}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid",             libs.androidx.room.compiler)
    add("kspIosX64",              libs.androidx.room.compiler)
    add("kspIosArm64",            libs.androidx.room.compiler)
    add("kspIosSimulatorArm64",   libs.androidx.room.compiler)
}