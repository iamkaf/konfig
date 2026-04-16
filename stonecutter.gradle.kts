plugins {
    id("dev.kikugie.stonecutter")
    id("com.iamkaf.multiloader.root")
    id("fabric-loom") version "1.15.5" apply false
}

stonecutter active "26.1.2"

val teakitRunClientPropertyNames = listOf(
    "teakit.worldId",
    "teakit.worldName",
    "teakit.port",
    "teakit.strictPort",
    "teakit.repoRoot",
    "teakit.scenarioRoot",
    "teakit.autoExitTitle",
    "teakit.autoExitTitleDelayMs"
)

subprojects {
    tasks.configureEach {
        if (name != "runClient") {
            return@configureEach
        }

        val systemPropertyMethod = javaClass.methods.firstOrNull { method ->
            method.name == "systemProperty" && method.parameterCount == 2
        }
        if (systemPropertyMethod == null) {
            return@configureEach
        }

        val autoWorld = System.getProperty("teakit.autoWorld")
        if (autoWorld != null) {
            systemPropertyMethod.invoke(this, "teakit.autoWorld", autoWorld)
        }
        teakitRunClientPropertyNames.forEach { propertyName ->
            val value = System.getProperty(propertyName)
            if (value != null) {
                systemPropertyMethod.invoke(this, propertyName, value)
            }
        }
    }
}
