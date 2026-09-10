plugins {
    id("wan.android.library")
    id("wan.hilt")
    alias(libs.plugins.serialization)
}
dependencies {
    implementation(libs.serialization)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter)
    implementation(libs.okhttp)
}
