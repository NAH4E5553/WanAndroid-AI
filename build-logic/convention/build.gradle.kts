plugins { `kotlin-dsl` }
group = "com.personal.wanandroid.buildlogic"
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
dependencies {
    compileOnly(libs.android.gradle)
    compileOnly(libs.kotlin.gradle)
}
gradlePlugin {
    plugins {
        register("wanApplication") {
            id = "wan.android.application"
            implementationClass = "WanApplicationPlugin"
        }
        register("wanLibrary") {
            id = "wan.android.library"
            implementationClass = "WanLibraryPlugin"
        }
        register("wanCompose") {
            id = "wan.android.compose"
            implementationClass = "WanComposePlugin"
        }
        register("wanHilt") {
            id = "wan.hilt"
            implementationClass = "WanHiltPlugin"
        }
        register("wanFeature") {
            id = "wan.android.feature"
            implementationClass = "WanFeaturePlugin"
        }
    }
}
