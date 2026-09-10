plugins {
    id("wan.android.library")
    id("wan.hilt")
    alias(libs.plugins.room)
}
room { schemaDirectory("$projectDir/schemas") }
dependencies {
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.coroutines)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
}
