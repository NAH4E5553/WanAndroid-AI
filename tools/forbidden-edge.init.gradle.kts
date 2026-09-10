// Negative verification only: this changes the in-memory test invocation, not build files.
// Run: ./gradlew verifyArchitecture -I tools/forbidden-edge.init.gradle.kts
// Expected failure: Forbidden edges: :feature:home -> [:feature:auth]
gradle.projectsEvaluated {
    val home = rootProject.findProject(":feature:home") ?: return@projectsEvaluated
    home.dependencies.add(
        "implementation",
        home.dependencies.project(mapOf("path" to ":feature:auth"))
    )
}
