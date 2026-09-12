import org.gradle.api.artifacts.ProjectDependency

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/.gradle/**")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", "**/.gradle/**")
        ktlint(libs.versions.ktlint.get())
    }
}

// Checks the actual configured project dependency graph, including convention-added edges.
tasks.register("verifyArchitecture") {
    group = "verification"
    description = "Reject forbidden module edges, DTO/DAO access from UI, and mall remnants."
    doLast {
        val allowed = mapOf(
            ":core" to emptySet(),
            ":feature" to emptySet(),
            ":core:common" to setOf(":core:result", ":core:model"),
            ":core:result" to emptySet(),
            ":core:model" to emptySet(),
            ":core:network" to emptySet(),
            ":core:database" to emptySet(),
            ":core:data" to setOf(":core:result", ":core:model", ":core:network", ":core:database"),
            ":core:navigation" to emptySet(),
            ":core:designsystem" to emptySet(),
            ":core:ui" to setOf(":core:model", ":core:designsystem", ":core:common", ":core:result")
        )
        subprojects.forEach { module ->
            // Only declared dependency scopes; Hilt's internal aggregation configurations
            // contain synthetic self-edges that are not architectural dependencies.
            val edges = module.configurations.filter {
                Regex(".*(api|implementation|compileOnly|runtimeOnly)$", RegexOption.IGNORE_CASE)
                    .matches(it.name)
            }.flatMap { configuration ->
                configuration.dependencies.withType<ProjectDependency>().map { it.path }
            }.toSet()
            val permitted = when {
                module.path == ":app" -> subprojects.filter {
                    it.path.startsWith(":feature:")
                }.map { it.path }.toSet() +
                    setOf(":core:data", ":core:navigation", ":core:designsystem", ":core:ui")

                module.path.startsWith(":feature:") -> setOf(
                    ":core:common",
                    ":core:result",
                    ":core:model",
                    ":core:data",
                    ":core:navigation",
                    ":core:designsystem",
                    ":core:ui"
                )

                else -> allowed[module.path] ?: error("Unclassified module: ${module.path}")
            }
            check(
                edges.all {
                    it in permitted
                }
            ) { "Forbidden edges: ${module.path} -> ${edges - permitted}" }
            module.fileTree("src") { include("**/*.kt") }.forEach { source ->
                val content = source.readText()
                check(!content.contains("com.joker.coolmall")) { "Source package leaked: $source" }
                check(!content.contains("GlobalScope")) { "Unowned coroutine: $source" }
                if (module.path == ":core:result") {
                    check(
                        !Regex(
                            "import (android\\.|androidx\\.|retrofit2\\.|.*core\\.(network|data|ui)\\.)"
                        ).containsMatchIn(content)
                    ) {
                        "Result contract imports platform or data implementation: $source"
                    }
                }
                if (module.path == ":core:common") {
                    check(
                        !Regex(
                            "import .*core\\.(data|network|database|navigation|ui)\\."
                        ).containsMatchIn(content)
                    ) {
                        "Common imports an implementation layer: $source"
                    }
                }
                if (module.path == ":core:ui") {
                    check(!Regex("import .*core\\.data\\.").containsMatchIn(content)) {
                        "UI imports Repository: $source"
                    }
                }
                if (module.path.startsWith(":feature:") || module.path == ":core:ui") {
                    check(
                        !Regex("import .*core\\.(network|database)\\.").containsMatchIn(content)
                    ) {
                        "UI bypasses Repository: $source"
                    }
                }
                if (module.path in
                    setOf(
                        ":core:result",
                        ":core:common",
                        ":core:model",
                        ":core:data",
                        ":core:network",
                        ":core:database"
                    )
                ) {
                    check(!Regex("import androidx\\.compose\\.").containsMatchIn(content)) {
                        "Data layer imports Compose: $source"
                    }
                }
            }
        }
    }
}

// Configure lazily as each platform plugin is applied, including future modules.
val testAll = tasks.register("testAll") { group = "verification" }
subprojects {
    val module = this
    plugins.withId("com.android.base") {
        testAll.configure { dependsOn("${module.path}:testDebugUnitTest") }
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        testAll.configure { dependsOn("${module.path}:test") }
    }
}
