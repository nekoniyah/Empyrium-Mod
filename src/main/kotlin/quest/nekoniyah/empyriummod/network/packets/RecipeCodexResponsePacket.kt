package quest.nekoniyah.empyriummod.network.packets

import kotlinx.serialization.Serializable

@Serializable
@JvmRecord
data class RecipeCodexPacketEntry(val itemKey: String, val max: Int, val current: Int, val cap: Int)

@JvmRecord
data class RecipeCodexResponsePacket(val items: List<RecipeCodexPacketEntry>)