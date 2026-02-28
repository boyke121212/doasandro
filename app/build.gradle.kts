plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "com.toelve.doas"
    compileSdk = 36
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.toelve.doas"
        minSdk = 24
        targetSdk = 36
        buildConfigField(
            "String",
            "BASE_URL",
            "\"https://dittipidter-doas.online/\""
        )
        buildConfigField(
            "String",
            "refresh",
            "\"api/refresh-token\""
        )
        buildConfigField(
            "String",
            "CHECKING",
            "\"api/auth-check\""
        )
        buildConfigField(
            "String",
            "DATAABSEN",
            "\"api/dataabsen\""
        )
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.googleLocation)
    implementation(libs.volley)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.androidx.biometric)
    implementation(libs.coil)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.photoview)
    implementation(libs.securityCrypto)
    testImplementation(libs.junit)
    implementation(libs.subsampling.scale.image.view)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.exifinterface.v137)
    implementation(libs.mpAndroidChart)
    implementation(libs.gson)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
