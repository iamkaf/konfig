import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("com.iamkaf.multiloader.common")
}

val nightConfigVersion = providers.gradleProperty("nightconfig_version").get()

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
}
