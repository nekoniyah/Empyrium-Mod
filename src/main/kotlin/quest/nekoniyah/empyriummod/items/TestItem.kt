package quest.nekoniyah.empyriummod.items

import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import quest.nekoniyah.empyriummod.items.ModItems

object TestItem : CustomItem("test", listOf(CreativeModeTabs.INGREDIENTS), Properties()) {
    val item: Item = ModItems.register(this, id)
}