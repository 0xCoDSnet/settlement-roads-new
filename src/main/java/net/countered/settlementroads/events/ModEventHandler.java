package net.countered.settlementroads.events;


import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.helpers.StructureLocator;
import net.countered.settlementroads.persistence.RoadData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.countered.settlementroads.SettlementRoads.MOD_ID;

public class ModEventHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static RoadData roadData;

    public static void register() {

        ServerWorldEvents.LOAD.register((minecraftServer, serverWorld) -> {
            if (!serverWorld.getRegistryKey().equals(World.OVERWORLD)) {
                return; // Only in Overworld
            }
            roadData = RoadData.getOrCreateRoadData(serverWorld);
            try {
                if (roadData.getStructureLocations().size() < ModConfig.initialLocatingCount) {
                    StructureLocator.locateConfiguredStructure(serverWorld, ModConfig.initialLocatingCount, false);
                }
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        });
        ServerWorldEvents.UNLOAD.register((minecraftServer, serverWorld) -> {
            if (!serverWorld.getRegistryKey().equals(World.OVERWORLD)) {
                return;
            }
            LOGGER.info("Clearing road cache...");
            RoadFeature.roadSegmentsCache.clear();
            RoadFeature.roadAttributesCache.clear();
            RoadFeature.roadChunksCache.clear();
        });
    }
}
