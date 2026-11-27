package quest.nekoniyah.empyriummod

import io.wispforest.owo.network.OwoNetChannel
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.storage.LevelResource
import net.minecraft.world.item.Rarity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import quest.nekoniyah.empyriummod.commands.EmpyriumCommand
import quest.nekoniyah.empyriummod.config.RecipeCodexConfig
import quest.nekoniyah.empyriummod.config.RecipeCodexEntry
import quest.nekoniyah.empyriummod.database.EmpyriumDatabase
import quest.nekoniyah.empyriummod.difficulty.DifficultyAttachments
import quest.nekoniyah.empyriummod.difficulty.EmpyriumDifficulty
import quest.nekoniyah.empyriummod.economy.CurrencyCommand
import quest.nekoniyah.empyriummod.economy.PlayerShopEvents
import quest.nekoniyah.empyriummod.economy.ShopCommand
import quest.nekoniyah.empyriummod.economy.currencies.CurrencyRegistry
import quest.nekoniyah.empyriummod.factions.utils.EmpyriumFactions
import quest.nekoniyah.empyriummod.factions.utils.FactionAttachments

object EmpyriumMod : ModInitializer {
    const val MOD_ID: String = "empyrium-mod"

    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    val difficulty = EmpyriumDifficulty(DifficultyAttachments())
    val factions = EmpyriumFactions(FactionAttachments())

    var server: MinecraftServer? = null

    val NET_CHANNEL = OwoNetChannel.create(id("main"))

    override fun onInitialize() {
        factions.register()

        RecipeCodexConfig.init()

        ServerLifecycleEvents.SERVER_STARTED.register { srv ->
            server = srv

            val dbDir = srv.serverDirectory.resolve("data").resolve(MOD_ID)
            EmpyriumDatabase.init(dbDir)
            
            if (!RecipeCodexConfig.checkEmptiness()) return@register

            val allRecipes = srv.recipeManager.recipes

            allRecipes.map { it.value.getResultItem(srv.registryAccess()) }.filter { stack -> !stack.isEmpty }
                .distinct().forEach { itemStack ->
                    val key = BuiltInRegistries.ITEM.getKey(itemStack.item)

                    val rarity = itemStack.rarity

                    val goal = when (rarity) {
                        Rarity.COMMON -> 25
                        Rarity.UNCOMMON -> 15
                        Rarity.RARE -> 7
                        Rarity.EPIC -> 3
                    }

                    val freeCount = when (rarity) {
                        Rarity.COMMON -> 10
                        Rarity.UNCOMMON -> 7
                        Rarity.RARE -> 3
                        Rarity.EPIC -> 1
                    }

                    RecipeCodexConfig.addOne(RecipeCodexEntry(key.toString(), goal, freeCount))
                }
        }

        ServerLifecycleEvents.SERVER_STOPPED.register {
            server = null
            EmpyriumDatabase.close()
        }

        ModItems.initialize()
        difficulty.register()

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(EmpyriumCommand(listOf(factions, difficulty, CurrencyCommand())).command)
            dispatcher.register(ShopCommand().command)
        }

        ServerPlayerEvents.JOIN.register { player ->
            player.setAttached(CurrencyRegistry[0].ATTACHMENT, EmpyriumDatabase.getBalance(player.stringUUID))
        }

        ServerPlayerEvents.COPY_FROM.register { _, player, _ ->
            player.setAttached(CurrencyRegistry[0].ATTACHMENT, EmpyriumDatabase.getBalance(player.stringUUID))
        }

        ServerTickEvents.END_SERVER_TICK.register { server ->
            for (player in server.playerList.players) {
                player.setAttached(CurrencyRegistry[0].ATTACHMENT, EmpyriumDatabase.getBalance(player.stringUUID))
            }
        }

        PlayerShopEvents.register()

        LOGGER.info("Hello Fabric world!")
    }


    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}
