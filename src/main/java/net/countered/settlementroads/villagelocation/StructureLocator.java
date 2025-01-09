package net.countered.settlementroads.villagelocation;

import net.countered.settlementroads.persistence.RoadData;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static net.countered.settlementroads.SettlementRoads.MOD_ID;

public class StructureLocator {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ServerWorld serverWorld;
    private final RoadData roadData;

    public StructureLocator(RoadData roadData, ServerWorld serverWorld) {
        this.serverWorld = serverWorld;
        this.roadData = roadData;
    }

    public void locateVillagesAsync(int count) {
        executor.submit(() -> {
            for (int i = 0; i < count; i++) {
                BlockPos structurePos = serverWorld.locateStructure(
                        StructureTags.VILLAGE,
                        serverWorld.getSpawnPos(),
                        100,
                        true
                );
                roadData.addLocation(structurePos);
                LOGGER.info("Located village: " + structurePos);
                LOGGER.info(roadData.getStructureLocations().toString());
            }
            shutdown();
        });
    }

    private void shutdown() {
        executor.shutdown();
        LOGGER.info("village locator finished");
    }
}
