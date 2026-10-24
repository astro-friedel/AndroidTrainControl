import com.android.build.gradle.ProguardFiles.getDefaultProguardFile
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude

plugins {
    alias(libs.plugins.android.application)
}
val lifecycleVersion = "2.8.0"
android {
    namespace = "com.controls.train"
    //noinspection GradleDependency
    compileSdk = 34

    defaultConfig {
        applicationId = "com.controls.train"
        minSdk = 32
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 54
        versionName = "4.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        //resValue = "string", "app_name", "Bluefruit Connect"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        /*release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }*/
        create("beta") {
            initWith(getByName("debug"))

            applicationIdSuffix = ".beta"
            resValue("string", "app_name", "BlueConnect Beta")
        }

        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    androidResources {
        noCompress += "zip"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.core)
    implementation(libs.constraintlayout)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.swiperefreshlayout)
    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.0") {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    androidTestImplementation(libs.junit.v115)
    // Support library
    implementation(libs.appcompat.v170)
    implementation(libs.material.v1120)
    implementation(libs.vectordrawable)
    implementation(libs.recyclerview)
    implementation(libs.percentlayout)
    implementation(libs.cardview)
    
    // Nordic
    implementation(libs.scanner)
    //implementation("no.nordicsemi.android.support.v18:scanner:1.6.0")

    //  Architecture Components
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    implementation(libs.lifecycle.viewmodel.savedstate)
    implementation(libs.lifecycle.common.java8)

    // LeakCanary
    debugImplementation(libs.leakcanary.android)

    implementation(libs.androidsvg.aar)
    //
    testImplementation(libs.junit)
    implementation(libs.work.runtime)

}