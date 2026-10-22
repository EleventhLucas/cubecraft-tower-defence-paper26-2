package dev.cubecrafttd.match

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.testing.TestingMapFactory
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.visual.TowerPath
import java.util.UUID
import kotlin.test.*

class ZeusSummonLifecycleTest {
    @Test fun onlyBabyZeusStagesRequireASummon() {
        val counts = RecommendedTowerSummonCountResolver.withExplicitSorcererCounts(emptyMap())
        val definition = RecommendedMatureTowerDefinitions.get("zeus")
        for (path in TowerPath.entries) for (level in 1..4) {
            val stage = TowerStageResolver.resolve(definition,path,level)
            assertEquals(if (path == TowerPath.BOTTOM && level >= 3) 1 else 0,
                counts.desiredCount("zeus",stage),"$path L$level")
        }
    }

    @Test fun repeatedSessionsCanBuildUpgradeAndRemoveZeusWithoutCrashing() {
        repeat(3) { round ->
            val context = ArenaContext(ArenaId("freeplay-zeus-$round"),UUID.randomUUID(),TestingMapFactory.minimal())
            val live = mutableSetOf<UUID>()
            val phase = TowerSummonMaintenancePhase(
                RecommendedTowerSummonCountResolver.withExplicitSorcererCounts(emptyMap()),
                TowerSummonSpawnPort { UUID.randomUUID().also { live += it } },
                TowerSummonRemovalPort { live.remove(it) }
            )
            for (path in TowerPath.entries) {
                val tower = TowerRuntimeState(
                    TowerIdentity(TowerInstanceId(1),"zeus",UUID.randomUUID(),TeamId.RED),
                    TowerUpgradeState(1,path),
                    TowerGeometryState(Vec3(0.0,0.0,0.0),Vec3(0.0,0.0,0.0),emptyList(),"regression")
                )
                context.entityIndex.registerTower(tower)
                for (level in 1..4) {
                    tower.upgrade.level = level
                    phase.tick(context)
                    phase.tick(context)
                    assertEquals(if (path == TowerPath.BOTTOM && level >= 3) 1 else 0,live.size)
                }
                context.entityIndex.unregisterTower(tower.identity.instanceId)
                phase.tick(context)
                assertEquals(0,phase.registrySize())
                assertTrue(live.isEmpty())
            }
            val gate = FarmReuseGate(entityPresence=TrackedEntityPresencePort { it in live })
            assertFalse(gate.record(ArenaTeardownReport(emptyList(),emptySet(),emptySet(),0,0)).blocked)
        }
    }
}
