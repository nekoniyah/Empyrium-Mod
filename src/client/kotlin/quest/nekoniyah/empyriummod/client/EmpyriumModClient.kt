package quest.nekoniyah.empyriummod.client

import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BeaconRenderer
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.client.events.KeybindEvents
import quest.nekoniyah.empyriummod.client.keymappings.EmpyriumKeyMappings
import quest.nekoniyah.empyriummod.client.overlays.EmpyriumOverlay
import quest.nekoniyah.empyriummod.client.screens.EmpyriumScreen
import quest.nekoniyah.empyriummod.client.screens.FactionSelectionScreen
import quest.nekoniyah.empyriummod.client.screens.PlayerShopScreen
import quest.nekoniyah.empyriummod.client.screens.RecipeCodexScreen
import quest.nekoniyah.empyriummod.client.screens.ShopResult
import quest.nekoniyah.empyriummod.client.util.ui.CurrencyHud
import quest.nekoniyah.empyriummod.economy.PlayerShopItem
import quest.nekoniyah.empyriummod.economy.currencies.CurrencyRegistry
import quest.nekoniyah.empyriummod.items.ModItems
import quest.nekoniyah.empyriummod.items.claim.ClaimSelectorItem
import quest.nekoniyah.empyriummod.network.packets.*

object EmpyriumModClient : ClientModInitializer {
    const val MOD_ID = "empyrium-mod"

    lateinit var keybinding: EmpyriumKeyMappings

    val screenRegistry: ArrayList<EmpyriumScreen> = arrayListOf(
        FactionSelectionScreen(), PlayerShopScreen(),
        RecipeCodexScreen()
    )
    var cachedPlayerShopItems: List<PlayerShopItem> = listOf()

    var codex: List<RecipeCodexPacketEntry> = listOf()

    override fun onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.

        keybinding = EmpyriumKeyMappings()

        EmpyriumMod.NET_CHANNEL.registerClientbound(OpenScreenPacket::class.java) { packet, access ->
            val screen = getScreen(packet.screenName) ?: return@registerClientbound
            Minecraft.getInstance().setScreen(screen)
        }

        KeybindEvents.register()

        CurrencyRegistry.forEach { c ->
            EmpyriumOverlay.addCurrencyData(
                CurrencyHud(
                    c.identifier, id("textures/item/${c.identifier}.png"), 0
                )
            )
        }

        EmpyriumMod.NET_CHANNEL.registerClientbound(PlayerShopResponsePacket::class.java) { packet, _ ->
            cachedPlayerShopItems = packet.items
            (Minecraft.getInstance().screen as? PlayerShopScreen)?.refresh()
        }

        EmpyriumMod.NET_CHANNEL.registerClientbound(PlayerShopMessagePacket::class.java) { packet, _ ->
            (Minecraft.getInstance().screen as? PlayerShopScreen)
                ?.onShopResult(ShopResult(packet.success, packet.message))
        }

        EmpyriumMod.NET_CHANNEL.registerClientbound(RecipeCodexResponsePacket::class.java) { packet, _ ->
            codex = packet.items
            (Minecraft.getInstance().screen as? RecipeCodexScreen)?.refresh()
        }

        ClientTickEvents.END_CLIENT_TICK.register { minecraft ->
            val player = minecraft.player ?: return@register

            CurrencyRegistry.forEach { c ->
                val currencyVal = player.getAttached(c.ATTACHMENT) ?: 0

                EmpyriumOverlay.updateCurrencyData(
                    CurrencyHud(
                        c.identifier, id("textures/item/${c.identifier}.png"), currencyVal
                    )
                )
            }

            EmpyriumOverlay.update()
        }

        EmpyriumOverlay.init()

        WorldRenderEvents.AFTER_ENTITIES.register(this::renderBeaconBeam) // Rendu du beacon
    }


    fun getScreen(identifier: String): EmpyriumScreen? {
        return screenRegistry.find { it.identifier == identifier }
    }

    fun renderBeaconBeam(context: WorldRenderContext) {

        var level: ClientLevel = context.world()
        var client: Minecraft = Minecraft.getInstance()
        val player: LocalPlayer? = client.player

        if (player == null) return // Peut s'écrire : val player : LocalPlayer? = client.player ?: return

        val stack = player.mainHandItem
        if (!stack.`is`(ModItems.CLAIM_SELECTOR)) return // Condition ItemStack.is(Item) mais en Kotlin is est utilisé par le langage alors il faut mettre des quotes

        val claimPoints: List<BlockPos>? = stack.get(ClaimSelectorItem.CLAIM_POINTS)

        if (claimPoints.isNullOrEmpty()) return // Vérifie si la liste des vides ou null

        val poseStack: PoseStack? = context.matrixStack() // Matrice d'affichage 
        val consumers: MultiBufferSource? = context.consumers()
        val cameraPos: Vec3 = context.camera().position
        val tickDelta: Float = context.tickCounter().getGameTimeDeltaPartialTick(true)
        val worldTime: Long = level.gameTime

        if (poseStack == null || consumers == null) return

        for (pos in claimPoints) {
            poseStack.pushPose()
            poseStack.translate(
                pos.x - cameraPos.x,
                pos.y - cameraPos.y,
                pos.z - cameraPos.z
            ) // Repositionnement de la matrice par rapport à la caméra

            BeaconRenderer.renderBeaconBeam(
                poseStack,
                consumers,
                BeaconRenderer.BEAM_LOCATION, // Texture officielle du faisceau
                tickDelta,                    // Interpolation fluide entre les ticks
                1.0f,                         // Échelle de hauteur (1.0 = standard)
                worldTime,                    // Temps du monde pour la rotation
                0,                            // Décalage Y de départ (0 = base du bloc)
                256,                          // Hauteur du faisceau en blocs
                0xFFFF0000.toInt(),           // Couleur ARGB (Cyan : .toInt() obligatoire en Kotlin)
                0.2f,                         // Rayon intérieur
                0.25f                         // Rayon extérieur translucide
            )

            poseStack.popPose()
        }
    }

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}