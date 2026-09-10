plugins {
    id("wan.android.application")
    id("wan.android.compose")
    id("wan.hilt")
}
android {
    namespace = "com.personal.wanandroid"
    defaultConfig {
        applicationId = "com.personal.wanandroid"
        versionCode = 1
        versionName = "0.1.0"
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
}
dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.navigation)
    implementation(projects.feature.home)
    implementation(projects.feature.topics)
    implementation(projects.feature.profile)
    implementation(projects.feature.article)
    implementation(projects.feature.auth)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core)
    implementation(libs.navigation.runtime)
    implementation(libs.navigation.ui)
    implementation(libs.androidx.lifecycle.navigation)
}
