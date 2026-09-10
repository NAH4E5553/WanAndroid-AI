import org.gradle.api.Plugin
import org.gradle.api.Project

class WanFeaturePlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("wan.android.library")
        pluginManager.apply("wan.android.compose")
        pluginManager.apply("wan.hilt")
        dependencies.add("implementation", catalog.findLibrary("hilt-compose").get())
        dependencies.add(
            "implementation",
            catalog.findLibrary("androidx-lifecycle-viewmodel").get()
        )
        dependencies.add("implementation", catalog.findLibrary("coroutines").get())
        dependencies.add("testImplementation", catalog.findLibrary("coroutines-test").get())
    }
}
