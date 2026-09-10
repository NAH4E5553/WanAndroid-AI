plugins {
    id("wan.android.library")
    alias(libs.plugins.serialization)
}
dependencies {
    api(libs.navigation.runtime)
    implementation(libs.serialization)
}
