import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class WanComposePlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        extensions.getByType<CommonExtension>().buildFeatures.compose = true
        dependencies.add(
            "implementation",
            dependencies.platform(catalog.findLibrary("compose-bom").get())
        )
        listOf(
            "compose-ui",
            "compose-material3",
            "compose-preview",
            "androidx-lifecycle-runtime"
        ).forEach {
            dependencies.add("implementation", catalog.findLibrary(it).get())
        }
        dependencies.add("debugImplementation", catalog.findLibrary("compose-tooling").get())
    }
}
