import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.Sync
import java.util.Properties

plugins {
    id("com.iamkaf.multiloader.fabric")
}

val nightConfigVersion = providers.gradleProperty("nightconfig_version").get()
val minecraftVersion = project.name
val isModernLine = !minecraftVersion.startsWith("1.")
val catalog = mcCatalog()
val modMenu = catalog.findLibrary("modmenu")
val modMenuVersion = catalog.findVersion("modmenu")
    .map { it.requiredVersion }
    .orElse(null)
val hasModMenu = modMenu.isPresent && !modMenuVersion.isNullOrBlank() && modMenuVersion != "null"
val useTeaKit = providers.systemProperty("konfig.withTeaKit")
    .orElse(providers.gradleProperty("konfig.withTeaKit"))
    .map { it.toBoolean() }
    .orElse(false)
    .get()
val usesOldModMenuApi = minecraftVersion in setOf("1.14.4", "1.15.2", "1.16", "1.16.1", "1.16.2", "1.16.3", "1.16.4")
val usesTerraformersLegacyModMenuApi = minecraftVersion in setOf("1.16.5", "1.17", "1.17.1")
val usesFabricOldRenderLegacyScreen = minecraftVersion in setOf("1.14.4", "1.15", "1.15.1", "1.15.2")
val usesFabricPoseStackLegacyScreen = minecraftVersion == "1.16" || minecraftVersion.startsWith("1.16.")

fun mcCatalog(): VersionCatalog {
    val catalogs = extensions.getByType<VersionCatalogsExtension>()
    val name = "libsMc${minecraftVersion.replace(".", "").replace("-", "")}"
    return catalogs.named(name)
}

tasks.named<Sync>("stageMergedJavaSources").configure {
    if (usesOldModMenuApi) {
        from(rootProject.file("fabric/src/legacy-pre1165/java"))
    }
    if (usesTerraformersLegacyModMenuApi) {
        from(rootProject.file("fabric/src/legacy-1.16.5/java"))
    }
    if (usesFabricOldRenderLegacyScreen) {
        from(rootProject.file("fabric/src/legacy-pre116/java"))
    }
    if (!(usesFabricOldRenderLegacyScreen || usesFabricPoseStackLegacyScreen)) {
        exclude("com/iamkaf/konfig/fabric/KonfigConfigScreen.java")
    }
    if (!hasModMenu) {
        exclude("com/iamkaf/konfig/fabric/KonfigModMenuApi.java")
    }
}

dependencies {
    if (isModernLine) {
        implementation(include("com.electronwill.night-config:core:$nightConfigVersion")!!)
        implementation(include("com.electronwill.night-config:toml:$nightConfigVersion")!!)
        if (hasModMenu) {
            compileOnly(modMenu.get())
            if (useTeaKit) {
                runtimeOnly(modMenu.get()) {
                    isTransitive = false
                }
            }
        }
    } else {
        implementation(include("com.electronwill.night-config:core:$nightConfigVersion")!!)
        implementation(include("com.electronwill.night-config:toml:$nightConfigVersion")!!)
        if (hasModMenu) {
            "modCompileOnly"(modMenu.get())
            "modLocalRuntime"(modMenu.get()) {
                isTransitive = false
            }
        }
    }
}
