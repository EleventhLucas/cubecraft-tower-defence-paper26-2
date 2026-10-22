package dev.cubecrafttd.match

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.progression.ProgressionMode

/** Local testing policy; not a reconstruction of historical CubeCraft rules. */
object FreeplayRules {
    const val STARTING_RESOURCES = 100_000_000L

    fun preset() = MatchRulePreset(
        MatchMode.FREEPLAY, PricingMode.NORMAL, GoldmineIncomeMode.NORMAL,
        ProgressionMode.ALL_UNLOCKED,
        MatchStartingBalance(STARTING_RESOURCES, STARTING_RESOURCES), 1L, 1L
    )

    fun attackedTeam(sender: TeamId, freeplay: Boolean): TeamId =
        if (freeplay) sender else if (sender == TeamId.RED) TeamId.BLUE else TeamId.RED
}
