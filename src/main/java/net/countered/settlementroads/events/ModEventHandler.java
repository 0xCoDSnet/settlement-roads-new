package net.countered.settlementroads.events;


import net.countered.settlementroads.features.config.RoadFeatureConfig;
import net.countered.settlementroads.features.roadlogic.Road;
import net.countered.settlementroads.features.roadlogic.RoadFeature;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.helpers.StructureConnector;
import net.countered.settlementroads.persistence.attachments.WorldDataAttachment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static net.countered.settlementroads.SettlementRoads.MOD_ID;

public class ModEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void register() {

        ServerWorldEvents.LOAD.register((minecraftServer, serverWorld) -> {
            Records.StructureLocationData villageLocationData = serverWorld.getAttachedOrCreate(WorldDataAttachment.STRUCTURE_LOCATIONS, () -> new Records.StructureLocationData(new ArrayList<>()));

            //List<BlockPos> villageLocations = villageLocationData.structureLocations();
            //if (villageLocations == null || villageLocations.size() < ModConfig.initialLocatingCount) {
            //    StructureLocator.locateConfiguredStructure(serverWorld, ModConfig.initialLocatingCount, false);
            //}
        });

        ServerTickEvents.END_WORLD_TICK.register((serverWorld) -> {
            if (!StructureConnector.cachedVillageConnections.isEmpty()) {
                Records.VillageConnection villageConnection = StructureConnector.cachedVillageConnections.poll();
                ConfiguredFeature<?, ?> feature = serverWorld.getRegistryManager()
                        .get(RegistryKeys.CONFIGURED_FEATURE)
                        .get(RoadFeature.ROAD_FEATURE_KEY);

                if (feature != null && feature.config() instanceof RoadFeatureConfig roadConfig) {
                    new Road(serverWorld, villageConnection, roadConfig).generateRoad();
                }
            }
        });

    }
}
