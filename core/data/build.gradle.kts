plugins {
    id("wan.android.library")
    id("wan.hilt")
}
dependencies {
    testImplementation(libs.okhttp)
    implementation(libs.serialization)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    implementation(projects.core.result)
    implementation(projects.core.model)
    implementation(projects.core.network)
    implementation(projects.core.database)
    implementation(libs.coroutines)
    implementation(libs.datastore.preferences)
    testImplementation(libs.coroutines.test)
}
