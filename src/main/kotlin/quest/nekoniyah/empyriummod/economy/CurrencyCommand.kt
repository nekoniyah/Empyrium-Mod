package quest.nekoniyah.empyriummod.economy

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.world.entity.player.Player
import quest.nekoniyah.empyriummod.commands.SubcommandHolder
import quest.nekoniyah.empyriummod.economy.currencies.Currency
import quest.nekoniyah.empyriummod.economy.currencies.CurrencyRegistry

class CurrencyCommand : SubcommandHolder("currency", true) {
    val giveMoneyCommandDefinition = Commands.literal("give").then(
        Commands.argument("amount", IntegerArgumentType.integer(0)).then(
            Commands.argument("currency", StringArgumentType.string()).suggests { _, builder ->
                return@suggests builder.also {
                    CurrencyRegistry.forEach { currency ->
                        it.suggest(currency.identifier)
                    }
                }.buildFuture()
            }.executes(::giveMoneySelfCommand).then(
                Commands.argument("player", EntityArgument.player()).executes(::giveMoneyCommand)
            )
        )
    )

    val removeMoneyCommandDefinition = Commands.literal("take").then(
        Commands.argument("amount", IntegerArgumentType.integer(0)).then(
            Commands.argument("currency", StringArgumentType.string()).suggests { _, builder ->
                return@suggests builder.also {
                    CurrencyRegistry.forEach { currency ->
                        it.suggest(currency.identifier)
                    }
                }.buildFuture()
            }.executes(::removeMoneySelfCommand).then(
                Commands.argument("player", EntityArgument.player()).executes(::removeMoneyCommand)
            )
        )
    )

    val setMoneyCommandDefinition = Commands.literal("set").then(
        Commands.argument("amount", IntegerArgumentType.integer(0)).then(
            Commands.argument("currency", StringArgumentType.string()).suggests { _, builder ->
                return@suggests builder.also {
                    CurrencyRegistry.forEach { currency ->
                        it.suggest(currency.identifier)
                    }
                }.buildFuture()
            }.executes(::setMoneySelfCommand).then(
                Commands.argument("player", EntityArgument.player()).executes(::setMoneyCommand)
            )
        )
    )


    fun giveMoneySelfCommand(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: return 0

        val currencyIdentifier = StringArgumentType.getString(ctx, "currency")
        val amount = IntegerArgumentType.getInteger(ctx, "amount")

        giveMoney(player, CurrencyRegistry.find { it.identifier == currencyIdentifier }!!, amount)

        return 1;
    }

    fun giveMoneyCommand(ctx: CommandContext<CommandSourceStack>): Int {
        val player = EntityArgument.getPlayer(ctx, "player")

        val currencyIdentifier = StringArgumentType.getString(ctx, "currency")
        val amount = IntegerArgumentType.getInteger(ctx, "amount")

        giveMoney(player, CurrencyRegistry.find { it.identifier == currencyIdentifier }!!, amount)

        return 1;
    }

    fun removeMoneySelfCommand(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: return 0

        val currencyIdentifier = StringArgumentType.getString(ctx, "currency")
        val amount = IntegerArgumentType.getInteger(ctx, "amount")

        removeMoney(player, CurrencyRegistry.find { it.identifier == currencyIdentifier }!!, amount)

        return 1;
    }

    fun removeMoneyCommand(ctx: CommandContext<CommandSourceStack>): Int {
        val player = EntityArgument.getPlayer(ctx, "player")

        val currencyIdentifier = StringArgumentType.getString(ctx, "currency")
        val amount = IntegerArgumentType.getInteger(ctx, "amount")

        removeMoney(player, CurrencyRegistry.find { it.identifier == currencyIdentifier }!!, amount)

        return 1;
    }

    fun setMoneySelfCommand(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: return 0

        val currencyIdentifier = StringArgumentType.getString(ctx, "currency")
        val amount = IntegerArgumentType.getInteger(ctx, "amount")

        setMoney(player, CurrencyRegistry.find { it.identifier == currencyIdentifier }!!, amount)

        return 1;
    }

    fun setMoneyCommand(ctx: CommandContext<CommandSourceStack>): Int {
        val player = EntityArgument.getPlayer(ctx, "player")

        val currencyIdentifier = StringArgumentType.getString(ctx, "currency")
        val amount = IntegerArgumentType.getInteger(ctx, "amount")

        setMoney(player, CurrencyRegistry.find { it.identifier == currencyIdentifier }!!, amount)

        return 1;
    }

    override var subcommands: List<ArgumentBuilder<CommandSourceStack, *>> =
        listOf(giveMoneyCommandDefinition, removeMoneyCommandDefinition, setMoneyCommandDefinition)

    fun giveMoney(player: Player, currency: Currency, amount: Int) {
        val playerCurrencyAmount = player.getAttached(currency.ATTACHMENT) ?: 0
        val newAmount = playerCurrencyAmount + amount
        player.setAttached(currency.ATTACHMENT, newAmount)
    }

    fun setMoney(player: Player, currency: Currency, amount: Int) {
        player.setAttached(currency.ATTACHMENT, amount)
    }

    fun removeMoney(player: Player, currency: Currency, amount: Int) {
        val playerCurrencyAmount = player.getAttached(currency.ATTACHMENT) ?: 0
        val newAmount = playerCurrencyAmount - amount
        player.setAttached(currency.ATTACHMENT, newAmount)
    }


    override fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        return 1
    }
}