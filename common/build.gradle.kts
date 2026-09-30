import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.GradleException
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult

plugins {
    id("com.iamkaf.multiloader.common")
}

val nightConfigVersion = providers.gradleProperty("nightconfig_version").get()

// JUnit 6 is Java 17 bytecode, so the Java 16 lines stay on JUnit 5.
val javaLanguageVersion = the<JavaPluginExtension>().toolchain.languageVersion.orNull?.asInt() ?: 17
val junitJupiterVersion = if (javaLanguageVersion >= 17) "6.1.0" else "5.13.4"
val junitLauncherVersion = if (javaLanguageVersion >= 17) "6.1.0" else "1.13.4"

if (project.parent?.name == "common") {
    val minecraftVersion = project.name
    val modName = providers.gradleProperty("mod.name").get()

    tasks.named<ProcessResources>("processResources").configure {
        inputs.property("minecraft_version", minecraftVersion)
        inputs.property("mod_name", modName)
        filesMatching("pack.mcmeta") {
            expand(
                mapOf(
                    "minecraft_version" to minecraftVersion,
                    "mod_name" to modName,
                ),
            )
        }
    }
}

dependencies {
    implementation("com.electronwill.night-config:core:$nightConfigVersion")
    implementation("com.electronwill.night-config:toml:$nightConfigVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitLauncherVersion")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.13")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) = Unit

        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null && result.testCount == 0L) {
                throw GradleException("Headless Konfig regression suite ran zero tests")
            }
        }

        override fun beforeTest(testDescriptor: TestDescriptor) = Unit

        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) = Unit
    })
}
