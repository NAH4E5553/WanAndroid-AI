import org.gradle.api.Plugin
import org.gradle.api.Project

class WanHiltPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("com.google.dagger.hilt.android")
        dependencies.add("implementation", catalog.findLibrary("hilt-android").get())
        dependencies.add("ksp", catalog.findLibrary("hilt-compiler").get())
    }
}
