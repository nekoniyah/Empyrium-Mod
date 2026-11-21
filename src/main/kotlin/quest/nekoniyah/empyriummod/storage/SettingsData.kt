package quest.nekoniyah.empyriummod.storage

import kotlinx.serialization.Serializable

@Serializable
data class SettingsData(val factions: ArrayList<FactionEntry>?)

@Serializable
data class FactionEntry(
    val name: String,
    val displayName: String,
    val description: String,
    val abilities: ArrayList<Ability>
)

@Serializable
data class Ability(
    val name: String? = null,
    val displayName: String?  = null,
    val description: String? = null,
    val identifier: String,
    val passive: Boolean = true,
    val keybind: Int? = null,
    val cooldown: Int? = null,
    val isAttribute: Boolean? = false,
    val mixin: String? = null
)