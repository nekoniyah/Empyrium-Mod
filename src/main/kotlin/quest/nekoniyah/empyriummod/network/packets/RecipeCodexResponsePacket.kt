package quest.nekoniyah.empyriummod.network.packets

import kotlinx.serialization.Serializable

@Serializable
@JvmRecord
data class RecipeCodexPacketEntry(val itemKey: String, val goal: Int, val current: Int, val totalFreeCount: Int, val usedFreeCount: Int)

@JvmRecord
data class RecipeCodexResponsePacket(val items: List<RecipeCodexPacketEntry>)