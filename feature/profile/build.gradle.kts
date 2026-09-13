plugins { id("wan.android.feature") }
dependencies {
    implementation(projects.core.result)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    debugImplementation(libs.compose.test.manifest)
    implementation(projects.core.navigation)
    implementation(libs.navigation.runtime)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.core.model)
    implementation(projects.core.data)
}
