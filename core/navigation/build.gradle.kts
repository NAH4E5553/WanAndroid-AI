plugins {
    id("wan.android.library")
    id("wan.hilt")
    alias(libs.plugins.serialization)
}
dependencies {
    api(libs.navigation.runtime)
    implementation(libs.serialization)
}
