package quest.nekoniyah.empyriummod.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import quest.nekoniyah.empyriummod.EmpyriumMod
import java.io.File

open class ConfigManager(val name: String) {
    val empyriumFolderPath = "./config/${EmpyriumMod.MOD_ID}/"
    val empyriumFolder = File(empyriumFolderPath)
    val configFile = File("$empyriumFolderPath$name.json")

    fun init() {
        if (!empyriumFolder.exists()) {
            empyriumFolder.mkdirs()
        }

        if (!configFile.exists()) {
            configFile.createNewFile()
        }

    }

    fun checkEmptiness(): Boolean {
        val content = configFile.readText()
        return content.isEmpty() || content == "{}"
    }


    fun getContent(): JsonObject {
        var c = configFile.readText()
        if (c.isEmpty()) c = "{}"

        return Json.parseToJsonElement(c).jsonObject
    }
}