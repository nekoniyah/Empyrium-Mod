package quest.nekoniyah.empyriummod.factions.powers

import net.minecraft.world.entity.player.Player
import quest.nekoniyah.empyriummod.storage.Ability

enum class AbilityExecutionResult {
    SUCCESS, DENIED, UNKNOWN, INVALID
}

enum class AttributeCalcType {
    MULTIPLY, ADD
}

data class AttributeCalc(val type: AttributeCalcType, val value: Float)

abstract class FactionAbility(val data: Ability) {
    open fun execute(player: Player): Any? = null

    fun checkAndExecute(player: Player): AbilityExecutionResult {
        if (data.isAttribute != null && data.isAttribute) {
            this.execute(player)
            return AbilityExecutionResult.SUCCESS
        }

        if (data.mixin != null) return AbilityExecutionResult.UNKNOWN

        if (getCooldown(player) != null) return AbilityExecutionResult.DENIED

        if (!data.passive) {
            if (data.cooldown == null) return AbilityExecutionResult.INVALID
            PowerCooldowns.add(PowerCooldown(player, data.cooldown * 20, data.identifier))
        }

        this.execute(player)
        return AbilityExecutionResult.SUCCESS
    }

    fun getCooldown(player: Player): PowerCooldown? {
        return PowerCooldowns.find { cooldown ->
            cooldown.player.stringUUID == player.stringUUID && cooldown.abilityIdentifier == data.identifier
        }
    }

    open fun unregister(player: Player): Any? = null
}