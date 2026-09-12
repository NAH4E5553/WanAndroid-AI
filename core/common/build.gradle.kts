plugins { id("wan.android.library") }
dependencies {
    implementation(projects.core.result)
    implementation(projects.core.model)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.coroutines)
    testImplementation(libs.coroutines.test)
}
