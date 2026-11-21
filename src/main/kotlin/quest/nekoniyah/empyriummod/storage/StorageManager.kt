package quest.nekoniyah.empyriummod.storage

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import kotlinx.serialization.json.Json
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.commands.SubcommandHolder
import java.io.File

class StorageManager : SubcommandHolder {
    val folderpath = "config/" + EmpyriumMod.MOD_ID
    val commonFilepath = "$folderpath/settings.json"
    val commonFile = File(commonFilepath)
    var data: SettingsData? = null

    constructor() : super("reload") {
        retrieve()
    }

    fun retrieve() {
        if (!File(folderpath).exists()) {
            File(folderpath).mkdirs()
        }

        if (!commonFile.exists()) {
            commonFile.createNewFile()
            commonFile.writeText("{}")

            data = SettingsData(arrayListOf())
        } else {
            data = Json.decodeFromString(commonFile.readText(Charsets.UTF_8))
        }
    }

    fun getFaction(name: String): FactionEntry? {
        if (data == null) return null
        if (data!!.factions == null) return null
        return this.data!!.factions!!.find { it.name == name }
    }

    override fun subcommands(): List<ArgumentBuilder<CommandSourceStack, *>> = emptyList()

    override fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        try {
            retrieve()
        } catch (e: Exception) {
            ctx.source.sendSystemMessage(Component.literal("Couldn't reload the settings: settings.json is invalid."))
        }
        return 0
    }
}