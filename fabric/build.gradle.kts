import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("com.iamkaf.multiloader.fabric")
}

val nightConfigVersion = providers.gradleProperty("nightconfig_version").get()
val minecraftVersion = project.name
val isModernLine = !minecraftVersion.startsWith("1.")
val modMenu = mcCatalog().findLibrary("modmenu").get()
val useTeaKit = providers.systemProperty("konfig.withTeaKit")
    .orElse(providers.gradleProperty("konfig.withTeaKit"))
    .map { it.toBoolean() }
    .orElse(false)
    .get()

fun mcCatalog(): VersionCatalog {
    val catalogs = extensions.getByType<VersionCatalogsExtension>()
    val name = "libsMc${minecraftVersion.replace(".", "").replace("-", "")}"
    return catalogs.named(name)
}

dependencies {
    implementation(include("com.electronwill.night-config:core:$nightConfigVersion")!!)
    implementation(include("com.electronwill.night-config:toml:$nightConfigVersion")!!)
    if (isModernLine) {
        compileOnly(modMenu)
        if (useTeaKit) {
            runtimeOnly(modMenu) {
                isTransitive = false
            }
        }
    } else {
        "modCompileOnly"(modMenu)
        "modLocalRuntime"(modMenu) {
            isTransitive = false
        }
    }
}
