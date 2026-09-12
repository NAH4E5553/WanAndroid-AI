plugins {
    id("wan.android.library")
    id("wan.android.compose")
}
dependencies {
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    debugImplementation(libs.compose.test.manifest)
    implementation(projects.core.common)
    implementation(projects.core.result)
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
}
