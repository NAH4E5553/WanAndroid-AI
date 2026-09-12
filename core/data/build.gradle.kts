plugins {
    id("wan.android.library")
    id("wan.hilt")
}
dependencies {
    implementation(projects.core.result)
    implementation(projects.core.model)
    implementation(projects.core.network)
    implementation(projects.core.database)
    implementation(libs.coroutines)
    implementation(libs.datastore.preferences)
    testImplementation(libs.coroutines.test)
}
