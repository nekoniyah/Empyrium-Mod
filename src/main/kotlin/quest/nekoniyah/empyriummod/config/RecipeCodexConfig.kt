package quest.nekoniyah.empyriummod.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json


@Serializable
data class RecipeCodexEntry(val key: String, val max: Int, val cap: Int)


object RecipeCodexConfig : ConfigManager("recipe-codex") {
    private val entriesSerializer =
        MapSerializer(String.serializer(), RecipeCodexEntry.serializer())

    fun addOne(entry: RecipeCodexEntry) {
        val entries = getAll()
        entries[entry.key] = entry // adds or replaces

        configFile.writeText(Json.encodeToString(entriesSerializer, entries))
    }

    fun getAll(): MutableMap<String, RecipeCodexEntry> =
        getContent()
            .mapValues { (_, value) ->
                Json.decodeFromJsonElement(RecipeCodexEntry.serializer(), value)
            }
            .toMutableMap()
}