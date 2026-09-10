plugins {
    id("wan.android.library")
    id("wan.android.compose")
}
dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
}
