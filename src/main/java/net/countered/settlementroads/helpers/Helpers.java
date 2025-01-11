package net.countered.settlementroads.helpers;

import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.events.ModEventHandler;
import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.persistence.RoadData;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Helpers {

    public static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

    public static void locateStructures(ServerWorld serverWorld, int count) {
        serverWorld.getServer().execute(() -> {
            LOGGER.info("Locating " + count + " structures...");
            for (int x = 0; x < count; x++) {
                BlockPos newVillageLocation = serverWorld.locateStructure(StructureTags.VILLAGE, serverWorld.getSpawnPos(), 50, true);
                if (newVillageLocation != null) {
                    LOGGER.info("Village found at " + newVillageLocation);
                    RoadData.getOrCreateRoadData(serverWorld).getStructureLocations().add(newVillageLocation);
                }
            }
        });
    }

    public static void loadChunkAtPositionAsync(WorldAccess worldAccess, BlockPos pos, ChunkStatus status) {

        int chunkX = ChunkPos.fromRegion(pos.getX(), pos.getZ()).x;
        int chunkZ = ChunkPos.fromRegion(pos.getX(), pos.getZ()).z;

        worldAccess.getServer().execute(() -> {
            LOGGER.info("Loading chunk at " + chunkX + ", " + chunkZ);
            worldAccess.getChunk(chunkX, chunkZ, status);
        });
    }
}
