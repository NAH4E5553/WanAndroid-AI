plugins { id("wan.android.feature") }
dependencies {
    implementation(projects.core.navigation)
    implementation(libs.navigation.runtime)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.core.model)
    implementation(projects.core.data)
}
