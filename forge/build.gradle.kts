import org.gradle.api.tasks.Sync

plugins {
    id("com.iamkaf.multiloader.forge")
}

val minecraftVersion = project.name
val useLegacy1165ForgeSources = minecraftVersion == "1.16.5"

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.logging.log4j" && requested.version == "2.11.+") {
            useVersion("2.11.2")
            because("Forge 1.20.1 userdev requests Log4j dynamically; pin it before repository metadata lookup.")
        }
        if (requested.group == "cpw.mods" && requested.name == "modlauncher") {
            if (requested.version == "9.0.+") {
                useVersion("9.0.24")
                because("Forge 1.18.2 userdev requests ModLauncher dynamically; pin it before repository metadata lookup.")
            }
            if (requested.version == "10.0.+") {
                useVersion("10.0.9")
                because("Forge 1.19-1.20 userdev requests ModLauncher dynamically; pin it before repository metadata lookup.")
            }
        }
    }
}

val forgeLibrariesRepository = repositories.maven {
    name = "ForgeLibraries"
    url = uri("https://maven.minecraftforge.net/")
    content {
        includeGroup("cpw.mods")
    }
}
repositories.remove(forgeLibrariesRepository)
repositories.addFirst(forgeLibrariesRepository)

if (useLegacy1165ForgeSources) {
    tasks.named<Sync>("stageMergedJavaSources").configure {
        from(rootProject.file("forge/src/legacy-1.16.5/java"))
        exclude("com/iamkaf/konfig/impl/v1/client/screen/KonfigConfigScreen.java")
    }
}
