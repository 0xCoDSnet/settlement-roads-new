package net.countered.settlementroads.events;


import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.helpers.StructureLocator;
import net.countered.settlementroads.persistence.RoadData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static net.countered.settlementroads.SettlementRoads.MOD_ID;

public class ModEventHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static RoadData roadData;

    public static boolean stopRecaching = false;

    public static void register() {

        ServerWorldEvents.LOAD.register((minecraftServer, serverWorld) -> {
            stopRecaching = false;
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
        ServerTickEvents.START_SERVER_TICK.register(server -> {

            if (ModConfig.loadRoadChunks){
                loadRoadChunksCompletely(server.getOverworld());
                if (!toRemove.isEmpty()) {
                    for (ChunkPos chunkPos : toRemove) {
                        if (server.getOverworld().getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FEATURES, false) != null) {
                            server.getOverworld().getChunkManager().removeTicket(ROAD_TICKET, chunkPos, 1, chunkPos.getStartPos());
                            RoadFeature.roadChunksCache.remove(chunkPos);
                            toRemove.remove(chunkPos);
                        }
                    }
                }
            }
            updateSigns(server.getOverworld());
            clearRoad(server.getOverworld());
        });
    }

    private static final ChunkTicketType<BlockPos> ROAD_TICKET = ChunkTicketType.create("road_ticket", Comparator.comparingLong(BlockPos::asLong));
    private static final Set<ChunkPos> toRemove = ConcurrentHashMap.newKeySet();

    private static void loadRoadChunksCompletely(ServerWorld serverWorld) {
        if (!RoadFeature.roadChunksCache.isEmpty()) {
            stopRecaching = true;
            RoadFeature.roadChunksCache.removeIf(chunkPos -> serverWorld.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FEATURES, false) != null);
            Queue<ChunkPos> toLoadQueue = new ConcurrentLinkedQueue<>(RoadFeature.roadChunksCache);
            int processed = 0;
            while (!toLoadQueue.isEmpty() /*&& processed < MAX_BLOCKS_PER_TICK*/) {
                ChunkPos roadChunkPos = toLoadQueue.poll();
                if (roadChunkPos != null) {
                    serverWorld.getChunkManager().addTicket(ROAD_TICKET, roadChunkPos, 1, roadChunkPos.getStartPos());
                    toRemove.add(roadChunkPos);
                }
                processed++;
            }
        }
    }

    private static final int MAX_BLOCKS_PER_TICK = 1;

    private static void clearRoad(ServerWorld serverWorld) {
        int processed = 0;
        while (!RoadFeature.roadPostProcessingPositions.isEmpty() && processed < MAX_BLOCKS_PER_TICK) {
            BlockPos roadBlockPos = RoadFeature.roadPostProcessingPositions.poll();
            if (roadBlockPos != null) {
                Block blockAbove = serverWorld.getBlockState(roadBlockPos.up()).getBlock();
                if (blockAbove == Blocks.SNOW) {
                    serverWorld.setBlockState(roadBlockPos.up(), Blocks.AIR.getDefaultState());
                }
            }
            processed++;
        }
    }

    private static void updateSigns(ServerWorld serverWorld) {
        int processed = 0;
        while (!RoadFeature.signPostProcessingPositions.isEmpty() && processed < MAX_BLOCKS_PER_TICK) {
            BlockPos signPos = RoadFeature.signPostProcessingPositions.poll();
            if (signPos != null) {
                BlockEntity entity = serverWorld.getBlockEntity(signPos);
                if (entity instanceof SignBlockEntity signEntity) {
                    signEntity.setText(new SignText().withMessage(1, Text.literal("ho")), true);
                    signEntity.markDirty();
                }
            }
            processed++;
        }
    }
}
