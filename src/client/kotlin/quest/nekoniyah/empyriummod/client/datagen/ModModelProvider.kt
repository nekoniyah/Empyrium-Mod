package quest.nekoniyah.empyriummod.client.datagen

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider
import net.minecraft.data.models.BlockModelGenerators
import net.minecraft.data.models.ItemModelGenerators
import net.minecraft.data.models.model.ModelTemplates
import quest.nekoniyah.empyriummod.items.ModItems

class ModModelProvider(output: FabricDataOutput) : FabricModelProvider(output) {
    override fun generateBlockStateModels(blockStateModelGenerator: BlockModelGenerators) {}

    override fun generateItemModels(itemModelGenerator: ItemModelGenerators) {

        itemModelGenerator.generateFlatItem(ModItems.CLAIM_SELECTOR, ModelTemplates.FLAT_ITEM)

    }
}