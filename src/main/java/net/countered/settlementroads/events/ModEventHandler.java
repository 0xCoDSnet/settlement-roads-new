package net.countered.settlementroads.events;


import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.helpers.StructureLocator;
import net.countered.settlementroads.persistence.RoadData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static net.countered.settlementroads.SettlementRoads.MOD_ID;

public class ModEventHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void register() {
        ServerWorldEvents.LOAD.register((minecraftServer, serverWorld) -> {
            try {
                onWorldLoaded(minecraftServer, serverWorld);
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        });
        ServerWorldEvents.UNLOAD.register((minecraftServer, serverWorld) -> {
            LOGGER.info("Clearing road cache...");
            RoadFeature.roadSegmentsCache.clear();
            RoadFeature.roadAttributesCache.clear();
            RoadFeature.roadChunksCache.clear();
        });
    }

    private static void onWorldLoaded(MinecraftServer minecraftServer, ServerWorld serverWorld) throws CommandSyntaxException {
        if (!RoadData.getOrCreateRoadData(serverWorld).getStructureLocations().isEmpty()) {
            return;
        }
        try {
            // Try as key first
            StructureLocator.locateStructures(serverWorld, ModConfig.structureToLocate, ModConfig.rawNumberOfStructures, false);
        } catch (IllegalArgumentException eKey) {
            try {
                // If key parsing fails, try as tag
                StructureLocator.locateStructures(serverWorld, ModConfig.structureToLocate, ModConfig.rawNumberOfStructures, true);
            } catch (Exception eTag) {
                LOGGER.error("Failed to locate structure as both key and tag: " + ModConfig.structureToLocate, eTag);
            }
        }
    }
}
