package quest.nekoniyah.empyriummod.factions.utils

import net.minecraft.world.entity.player.Player

enum class AbilityExecutionResult {
    SUCCESS, DENIED, UNKNOWN, INVALID
}

enum class AttributeCalcType {
    MULTIPLY, ADD
}

data class AttributeCalc(val type: AttributeCalcType, val value: Float)

abstract class FactionAbility(
    val displayName: String? = null,
    val description: String? = null,
    val identifier: String,
    val passive: Boolean = true,
    val keybind: Int? = null,
    val cooldown: Int? = null,
    val isAttribute: Boolean? = false,
    val mixin: String? = null,
    val damageTags: List<String>? = null
) {
    open fun execute(player: Player): Any? = null

    fun checkAndExecute(player: Player): AbilityExecutionResult {
        if (isAttribute != null && isAttribute) {
            this.execute(player)
            return AbilityExecutionResult.SUCCESS
        }

        if (mixin != null) return AbilityExecutionResult.UNKNOWN

        if (getCooldown(player) != null) return AbilityExecutionResult.DENIED

        if (!passive) {
            if (cooldown == null) return AbilityExecutionResult.INVALID
            PowerCooldowns.add(PowerCooldown(player, cooldown * 20, identifier))
        }

        this.execute(player)
        return AbilityExecutionResult.SUCCESS
    }

    fun getCooldown(player: Player): PowerCooldown? {
        return PowerCooldowns.find { cooldown ->
            cooldown.player.stringUUID == player.stringUUID && cooldown.abilityIdentifier == identifier
        }
    }

    open fun unregister(player: Player): Any? = null
}