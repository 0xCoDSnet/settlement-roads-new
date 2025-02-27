package net.countered.settlementroads.events;


import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.features.RoadStructures;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.helpers.StructureLocator;
import net.countered.settlementroads.persistence.RoadData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HangingSignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static net.countered.settlementroads.SettlementRoads.MOD_ID;

public class ModEventHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Map<RegistryKey<World>, RoadData> roadDataMap = new ConcurrentHashMap<>();

    public static boolean stopRecaching = false;

    public static void register() {

        ServerWorldEvents.LOAD.register((minecraftServer, serverWorld) -> {
            stopRecaching = false;
            //if (!serverWorld.getRegistryKey().equals(World.OVERWORLD)) {
            //    return; // Only in Overworld
            //}
            RoadData roadData = getRoadData(serverWorld);
            if (roadData == null) {
                return;
            }
            try {
                if (roadData.getStructureLocations().size() < ModConfig.initialLocatingCount) {
                    StructureLocator.locateConfiguredStructure(serverWorld, ModConfig.initialLocatingCount, false);
                }
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        });
        ServerWorldEvents.UNLOAD.register((minecraftServer, serverWorld) -> {
            //if (!serverWorld.getRegistryKey().equals(World.OVERWORLD)) {
            //    return;
            //}
            LOGGER.info("Clearing road cache...");
            roadDataMap.clear();
            RoadFeature.roadSegmentsCache.clear();
            RoadFeature.roadAttributesCache.clear();
            RoadFeature.roadChunksCache.clear();
        });
        ServerChunkEvents.CHUNK_GENERATE.register(ModEventHandler::clearRoad);
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            server.getWorlds().forEach(serverWorld -> {
                if (ModConfig.loadRoadChunks){
                    loadRoadChunksCompletely(serverWorld);
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
                placeDecorations(serverWorld);
                updateSigns(serverWorld);
            });
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

    private static void clearRoad(ServerWorld serverWorld, WorldChunk worldChunk) {
        if (RoadFeature.roadPostProcessingPositions.isEmpty()) {
            return;
        }
        for (BlockPos postProcessingPos : RoadFeature.roadPostProcessingPositions) {
            if (postProcessingPos != null) {
                if (!worldChunk.getPos().equals(new ChunkPos(postProcessingPos))) {
                    continue;
                }
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

    private static void updateSigns(ServerWorld serverWorld) {
        int processed = 0;
        while (!RoadFeature.signPostProcessingPositions.isEmpty() && processed < MAX_BLOCKS_PER_TICK) {
            Map.Entry<BlockPos, String> entry = RoadFeature.signPostProcessingPositions.poll();
            if (entry != null) {
                BlockPos signPos = entry.getKey();
                String text = entry.getValue();

                BlockEntity entity = serverWorld.getBlockEntity(signPos);
                if (entity instanceof HangingSignBlockEntity signEntity) {
                    SignText signText = signEntity.getText(true);
                    signText = (signText.withMessage(0, Text.literal("----------")));
                    signText = (signText.withMessage(1, Text.literal("Next Village")));
                    signText = (signText.withMessage(2, Text.literal(text+"m")));
                    signText = (signText.withMessage(3, Text.literal("----------")));
                    signEntity.setText(signText, true);

                    SignText signTextBack = signEntity.getText(false);
                    signTextBack = signTextBack.withMessage(0, Text.of("----------"));
                    signTextBack = signTextBack.withMessage(1, Text.of("Welcome"));
                    signTextBack = signTextBack.withMessage(2, Text.of("traveller"));
                    signTextBack = signTextBack.withMessage(3, Text.of("----------"));
                    signEntity.setText(signTextBack, false);

                    signEntity.markDirty();
                }
            }
            processed++;
        }
    }

    private static void placeDecorations(ServerWorld serverWorld) {
        if (RoadFeature.decorationPlacementPositions.isEmpty()) {
            return;
        }
        for (Records.RoadDecoration roadDecoration : RoadFeature.decorationPlacementPositions) {
            if (roadDecoration != null) {
                BlockPos placePos = roadDecoration.placePos();
                int centerBlockCount = roadDecoration.centerBlockCount();
                List<BlockPos> middleBlockPositions = roadDecoration.middleBlockPositions();
                Vec3i orthogonalVector = roadDecoration.vector();
                // place distance sign
                if (centerBlockCount == 10){
                    RoadStructures.placeDistanceSign(serverWorld, placePos, orthogonalVector, 1, true, String.valueOf(middleBlockPositions.size()));
                }
                if (centerBlockCount == middleBlockPositions.size()-10) {
                    RoadStructures.placeDistanceSign(serverWorld, placePos, orthogonalVector, 1, false, String.valueOf(middleBlockPositions.size()));
                }
                // place lantern
                if (centerBlockCount % 60 == 0){
                    RoadStructures.placeLantern(serverWorld, placePos, orthogonalVector, 1, true);
                }
                RoadFeature.decorationPlacementPositions.remove(roadDecoration);
            }
        }
    }
    public static RoadData getRoadData(ServerWorld serverWorld) {
        if (serverWorld.getDimension().hasCeiling()) {
            return null;
        }
        return roadDataMap.computeIfAbsent(serverWorld.getRegistryKey(),
                key -> RoadData.getOrCreateRoadData(serverWorld));
    }
}
