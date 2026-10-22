package dev.cubecrafttd.match

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.castle.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.player.PlayerRecoveryCoordinator
import dev.cubecrafttd.progression.*
import dev.cubecrafttd.testing.TestingMapFactory
import dev.cubecrafttd.troop.*
import dev.cubecrafttd.ui.*
import java.util.UUID
import kotlin.test.*

class FreeplayTest {
    private val player = UUID.fromString("00000000-0000-0000-0000-000000099001")
    private fun context() = ArenaContext(ArenaId("freeplay-test"), UUID.randomUUID(), TestingMapFactory.minimal())

    @Test fun soloBootstrapCapturesOnlyRealPlayerAndUnlocksEveryTroop() {
        val captured = mutableSetOf<UUID>()
        val recovery = object : PlayerRecoveryCoordinator {
            override fun captureBeforeMatch(playerUuid: UUID, arenaTick: Long) { captured += playerUuid }
            override fun restoreIfPossible(playerUuid: UUID) = captured.remove(playerUuid)
            override fun pending() = captured.toSet()
        }
        val arena = context()
        val ledger = EconomyLedger()
        val bootstrap = MatchBootstrapService(recovery, ledger, TroopProgressionService(ledger))
        val report = bootstrap.prepare(arena, NormalArenaRuntimeState.withCadence(TroopSpawnCadence(5, "test")),
            MatchBootstrapRequest(setOf(player), emptySet(), FreeplayRules.preset(), 20, 100))
        bootstrap.start(arena)
        assertEquals(setOf(player), captured)
        assertEquals(setOf(player), arena.redTeam.players)
        assertTrue(arena.blueTeam.players.isEmpty())
        assertEquals(ArenaState.RUNNING, arena.state)
        for (currency in listOf(EconomyCurrency.MATCH_COINS, EconomyCurrency.MATCH_EXP))
            assertEquals(FreeplayRules.STARTING_RESOURCES, ledger.balance(EconomyAccount(TeamId.RED, player, currency)))
        assertTrue(RecommendedMatureMobDefinitions.all().keys.all { report.session.players.getValue(player).progression.level(it) == 5 })
        assertTrue(recovery.restoreIfPossible(player))
        assertTrue(recovery.pending().isEmpty())
    }

    @Test fun freeplayRosterDoesNotWeakenVersusRequirements() {
        TeamRosterPolicy.validate(setOf(player), emptySet(), freeplay=true)
        assertFailsWith<IllegalArgumentException> { TeamRosterPolicy.validate(setOf(player), emptySet()) }
        assertFailsWith<IllegalArgumentException> { TeamRosterPolicy.validate(setOf(player), setOf(player), freeplay=true) }
    }

    @Test fun summonerTargetsOwnLaneRepeatedlyAndAvailabilityAgrees() {
        val arena = context()
        arena.redTeam.players += player
        arena.state = ArenaState.RUNNING
        val ledger = EconomyLedger()
        RecommendedMaturePricing.seedPlayer(ledger, TeamId.RED, player, FreeplayRules.preset().startingBalance, 1, "seed")
        val runtime = NormalArenaRuntimeState.withCadence(TroopSpawnCadence(5,"test"))
        val progression = TroopProgressionService(ledger)
        val state = TroopProgressionState()
        progression.initialize(state, ProgressionMode.ALL_UNLOCKED)
        val session = MatchSessionState(FreeplayRules.preset(), linkedMapOf(player to PlayerMatchSessionState(player,state)))
        val cooldowns = DeterministicCooldownTracker()
        val router = MatchMenuActionRouter(arena,runtime,session,ledger,progression,cooldowns,initialSequence=1000)
        repeat(2) {
            router.handle(MenuActionInvocation(player,"summoner:mob:zombie:l5",ClickKind.LEFT))
            val draft = session.players.getValue(player).interaction.summonerDraft
            assertTrue(SummonerSendAvailabilityEvaluator.evaluate(draft,runtime.queues.getValue(TeamId.RED),
                TeamId.RED,player,ledger,cooldowns,arena.gameTick,freeplay=true).canSend)
            val result = router.handle(MenuActionInvocation(player,"summoner:send",ClickKind.LEFT)) as MatchMenuActionResult.SummonerBatchSent
            assertEquals(TeamId.RED,result.receipt.attackedTeam)
            assertEquals(arena.gameTick,result.receipt.cooldownReadyAtTick)
        }
        assertEquals(2,runtime.queues.getValue(TeamId.RED).usedUnits())
        assertEquals(0,runtime.queues.getValue(TeamId.BLUE).usedUnits())
        assertEquals(TeamId.BLUE,FreeplayRules.attackedTeam(TeamId.RED,false))
    }

    @Test fun castleProtectionIsExplicitAndNormalCastleStillDies() {
        val protected = CastleRuntime(TeamId.RED,invulnerable=true)
        protected.damage(Double.MAX_VALUE)
        assertEquals(1000.0,protected.health)
        assertEquals(CastleState.ACTIVE,protected.state)
        val normal = CastleRuntime(TeamId.BLUE)
        normal.damage(1000.0)
        assertEquals(CastleState.DESTROYED,normal.state)
    }

    @Test fun selfSentTroopDeathRewardsThePlayersActualAccount() {
        val arena = context()
        arena.redTeam.players += player
        val ledger = EconomyLedger()
        val reward = SentMobExpRewardService(RecommendedMatureMobDefinitions, ledger, PricingMode.NORMAL, freeplay=true)
        val finalizer = PlayerSentMobDeathFinalizer(reward)
        val mob = MobRuntimeState(
            MobIdentity(MobInstanceId(10),UUID.randomUUID(),"zombie",player,TeamId.RED,1),
            MobRouteState("red-route",0,0.0,0.0),
            MobCombatState(0.0,40.0,MobLifecycleState.DEAD)
        )
        finalizer.finalize(arena,mob)
        val expected = RecommendedMatureMobDefinitions.get("zombie").level(1).expRewardOnDeath
        assertTrue(expected > 0L)
        assertEquals(expected,ledger.balance(EconomyAccount(TeamId.RED,player,EconomyCurrency.MATCH_EXP)))
        assertEquals(0L,ledger.balance(EconomyAccount(TeamId.BLUE,player,EconomyCurrency.MATCH_EXP)))
    }

    @Test fun freeplayContinuesPastArmageddonAndHardDeadline() {
        val arena = context()
        var starts = 0
        fun clock(freeplay: Boolean) = NormalMatchClockRuntime(0,
            ResolvedArmageddonSelection(ArmageddonType.WITHER,ArmageddonSelectionSource.ENGINEERING_TEST),
            TimeoutTiePolicy.DRAW,ArmageddonStartPort { _,_,_ -> starts++ },freeplay=freeplay)
        val sandbox = clock(true)
        arena.advanceSyntheticTick(100_000)
        assertEquals(MatchOutcome.Continue,sandbox.tick(arena,emptyList()).outcome)
        assertEquals(0,starts)
        assertFalse(sandbox.isArmageddonStarted())
        assertFalse(clock(false).tick(arena,emptyList()).outcome is MatchOutcome.Continue)
        assertEquals(1,starts)
    }
}
