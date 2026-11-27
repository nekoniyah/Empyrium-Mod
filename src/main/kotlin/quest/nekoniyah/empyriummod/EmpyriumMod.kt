package quest.nekoniyah.empyriummod

import io.wispforest.owo.network.OwoNetChannel
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.storage.LevelResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import quest.nekoniyah.empyriummod.commands.EmpyriumCommand
import quest.nekoniyah.empyriummod.database.EmpyriumDatabase
import quest.nekoniyah.empyriummod.difficulty.DifficultyAttachments
import quest.nekoniyah.empyriummod.difficulty.EmpyriumDifficulty
import quest.nekoniyah.empyriummod.economy.CurrencyCommand
import quest.nekoniyah.empyriummod.economy.ShopCommand
import quest.nekoniyah.empyriummod.economy.currencies.CurrencyRegistry
import quest.nekoniyah.empyriummod.factions.utils.EmpyriumFactions
import quest.nekoniyah.empyriummod.factions.utils.FactionAttachments
import quest.nekoniyah.empyriummod.network.packets.OpenScreenPacket
import quest.nekoniyah.empyriummod.network.packets.PlayerShopRequestPacket
import quest.nekoniyah.empyriummod.network.packets.PlayerShopResponsePacket

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

        ServerLifecycleEvents.SERVER_STARTED.register { srv ->
            server = srv

            val dbDir = srv.getWorldPath(LevelResource.ROOT).resolve(MOD_ID)
            EmpyriumDatabase.init(dbDir)
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
            EmpyriumDatabase.sellItem(player, ItemStack(Items.ACACIA_LOG), CurrencyRegistry[0], 10)
            EmpyriumDatabase.cachePlayer(player)
            sendPlayerShopUpdate(player, 0)
        }

        NET_CHANNEL.registerClientboundDeferred(OpenScreenPacket::class.java)

        NET_CHANNEL.registerServerbound(PlayerShopRequestPacket::class.java) { packet, source ->
            sendPlayerShopUpdate(source.player, packet.page)
        }

        LOGGER.info("Hello Fabric world!")
    }

    fun sendPlayerShopUpdate(player: Player, page: Int) {
        val allItems = EmpyriumDatabase.getItems()
        val pageSize = 20

        val endIndex = if (allItems.size < pageSize) allItems.size else page * pageSize + pageSize
        val startIndex = if (allItems.size < pageSize) 0 else page * pageSize

        val slice = allItems.subList(startIndex, endIndex)

        NET_CHANNEL.serverHandle(player).send(PlayerShopResponsePacket(page, slice))
    }

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}
