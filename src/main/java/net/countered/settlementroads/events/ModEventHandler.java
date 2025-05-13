package net.countered.settlementroads.events;


import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.features.roadlogic.RoadFeature;
import net.countered.settlementroads.helpers.StructureLocator;
import net.countered.settlementroads.persistence.VillageLocationData;
import net.countered.settlementroads.persistence.WorldDataAttachment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static net.countered.settlementroads.SettlementRoads.MOD_ID;

public class ModEventHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean stopRecaching = false;

    public static void register() {

        ServerWorldEvents.LOAD.register((minecraftServer, serverWorld) -> {
            VillageLocationData villageLocationData = serverWorld.getAttachedOrCreate(WorldDataAttachment.VILLAGE_LOCATIONS, () -> new VillageLocationData(new ArrayList<>()));
            List<BlockPos> villageLocations = villageLocationData.getVillages();
            if (villageLocations == null || villageLocations.size() < ModConfig.initialLocatingCount) {
                StructureLocator.locateConfiguredStructure(serverWorld, ModConfig.initialLocatingCount, false);
            }
        });
        ServerWorldEvents.UNLOAD.register((minecraftServer, serverWorld) -> {
            LOGGER.info("Clearing road cache...");
            RoadFeature.roadSegmentsCache.clear();
            RoadFeature.roadAttributesCache.clear();
            RoadFeature.roadChunksCache.clear();
        });
        ServerChunkEvents.CHUNK_LOAD.register(ModEventHandler::clearRoad);
    }

    private static void clearRoad(ServerWorld serverWorld, WorldChunk worldChunk) {
        if (RoadFeature.roadPostProcessingPositions.isEmpty()) {
            return;
        }
        for (BlockPos postProcessingPos : RoadFeature.roadPostProcessingPositions) {
            if (postProcessingPos != null) {
                Block blockAbove = worldChunk.getBlockState(postProcessingPos.up()).getBlock();
                Block blockAtPos = worldChunk.getBlockState(postProcessingPos).getBlock();
                if (blockAbove == Blocks.SNOW) {
                    worldChunk.setBlockState(postProcessingPos.up(), Blocks.AIR.getDefaultState(), false);
                    if (blockAtPos == Blocks.GRASS_BLOCK) {
                        worldChunk.setBlockState(postProcessingPos, Blocks.GRASS_BLOCK.getDefaultState(), false);
                    }
                }
                RoadFeature.roadPostProcessingPositions.remove(postProcessingPos);
            }
        }
    }
}
