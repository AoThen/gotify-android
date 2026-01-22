@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val currentDate: String by lazy {
    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
}

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jmailen.kotlinter") version "5.1.1"
}

android {
    namespace = "com.github.gotify"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.github.gotify"
        minSdk = 23
        targetSdk = 36
        versionCode = 34
        versionName = currentDate
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        signingConfig = signingConfigs.getByName("debug")
        resValue("string", "app_name", "Gotify")
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("SIGN_STORE_FILE") ?: "debug.keystore")
            storePassword = System.getenv("SIGN_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("SIGN_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("SIGN_KEY_PASSWORD") ?: ""
        }
        create("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("development") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        register("development") {
            applicationIdSuffix = ".dev"
            isDebuggable = true
            signingConfig = signingConfigs.getByName("development")
            resValue("string", "app_name", "Gotify DEV")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
            resValue("string", "app_name", "Gotify DEBUG")
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    packaging {
        resources {
            excludes.add("META-INF/DEPENDENCIES")
        }
    }
    lint {
        disable.add("GoogleAppIndexingWarning")
        lintConfig = file("../lint.xml")
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val apkName = "gotify-${variant.name}-$currentDate.apk"
            output.outputFileName = apkName
        }
    }
}



dependencies {
    val coilVersion = "2.7.0"
    val markwonVersion = "4.6.2"
    val tinylogVersion = "2.7.0"
    implementation(project(":client"))
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.vectordrawable:vectordrawable:1.2.0")
    implementation("androidx.preference:preference-ktx:1.2.1")

    implementation("com.github.cyb3rko:QuickPermissions-Kotlin:1.1.6")
    implementation("io.coil-kt:coil:$coilVersion")
    implementation("io.coil-kt:coil-svg:$coilVersion")
    implementation("io.noties.markwon:core:$markwonVersion")
    implementation("io.noties.markwon:image-coil:$markwonVersion")
    implementation("io.noties.markwon:image:$markwonVersion")
    implementation("io.noties.markwon:ext-tables:$markwonVersion")
    implementation("io.noties.markwon:ext-strikethrough:$markwonVersion")

    implementation("org.tinylog:tinylog-api-kotlin:$tinylogVersion")
    implementation("org.tinylog:tinylog-impl:$tinylogVersion")

    implementation("com.google.code.gson:gson:2.13.1")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("org.threeten:threetenbp:1.7.1")
    implementation("dnsjava:dnsjava:3.5.3")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

configurations {
    configureEach {
        exclude(group = "androidx.lifecycle", module = "lifecycle-viewmodel-ktx")
    }
}
