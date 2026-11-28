package quest.nekoniyah.empyriummod.economy.shop

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.serialization.JsonOps
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import quest.nekoniyah.empyriummod.commands.SubcommandHolder
import quest.nekoniyah.empyriummod.database.economy.AdminShopCategoryTable
import quest.nekoniyah.empyriummod.database.economy.AdminShopTable
import quest.nekoniyah.empyriummod.economy.currencies.CurrencyRegistry

class EditAdminShopCommand : SubcommandHolder("adminshop", true) {
    override fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        return 1
    }

    val categoryCommand: LiteralArgumentBuilder<CommandSourceStack> = Commands.literal("category").then(
        Commands.literal("add").then(
            Commands.argument(
                "name", StringArgumentType.string()
            ).executes(::executeAddCategoryDefIcon)
        )
    )

    val itemCommand: LiteralArgumentBuilder<CommandSourceStack> = Commands.literal("add").then(
        Commands.argument("categoryName", StringArgumentType.string()).suggests { context, builder ->
            builder.also { builder ->
                AdminShopCategoryTable.getCategories().forEach { categoryInfo ->
                    builder.suggest(categoryInfo.name)
                }
            }.buildFuture()
        }.then(
            Commands.argument("sellprice", IntegerArgumentType.integer()).then(
                Commands.argument("buyprice", IntegerArgumentType.integer()).executes(::executeAddItemToCategory)
            )
        )
    )


    fun executeAddItemToCategory(ctx: CommandContext<CommandSourceStack>): Int {
        val category = StringArgumentType.getString(ctx, "categoryName")
        val sellPrice = IntegerArgumentType.getInteger(ctx, "sellprice")
        val buyPrice = IntegerArgumentType.getInteger(ctx, "buyprice")

        val player = ctx.source.player ?: return 0

        val itemInHand = player.mainHandItem

        if (itemInHand.`is`(Items.AIR)) {
            player.sendSystemMessage(Component.literal("Hold an item to put in the admin shop!"))
            return 0
        }

        val jsonElement = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, itemInHand).resultOrPartial().orElseThrow()

        val itemString = jsonElement.asJsonObject.toString()

        val ok = AdminShopTable.addItem(
            itemString,
            itemInHand.count,
            CurrencyRegistry.first().identifier,
            AdminShopCategoryTable.getCategories().find { it.name == category }!!.id,
            if (sellPrice == 0) null else sellPrice,
            if (buyPrice == 0) null else buyPrice
        )

        return if (ok) 1 else 0
    }

    fun executeAddCategoryDefIcon(ctx: CommandContext<CommandSourceStack>): Int {
        val name = StringArgumentType.getString(ctx, "name")
        AdminShopCategoryTable.createCategory(name) ?: return 0

        return 1

    }

    override var subcommands: List<ArgumentBuilder<CommandSourceStack, *>> = listOf(categoryCommand, itemCommand)
}