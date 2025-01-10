package net.countered.settlementroads.events;


import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.features.RoadFeatureConfig;
import net.countered.settlementroads.persistence.RoadData;
import net.countered.settlementroads.villagelocation.StructureLocator;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.SquarePlacementModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;

import static net.countered.settlementroads.SettlementRoads.MOD_ID;

public class ModEventHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final int locateCount = 10;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(ModEventHandler::onServerStarted);
    }

    private static void onServerStarted(MinecraftServer server) {
        ServerWorld serverWorld = server.getOverworld();
        RoadData roadData = RoadData.getOrCreateRoadData(serverWorld);
        StructureLocator villageLocator = new StructureLocator(roadData, serverWorld);
        villageLocator.locateVillagesAsync(locateCount);
        LOGGER.info("Locating "+locateCount+" villages");
    }
}
