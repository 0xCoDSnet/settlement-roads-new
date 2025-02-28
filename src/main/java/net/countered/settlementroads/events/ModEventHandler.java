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
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HangingSignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static net.countered.settlementroads.SettlementRoads.MOD_ID;

public class ModEventHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Map<RegistryKey<World>, RoadData> roadDataMap = new ConcurrentHashMap<>();

    public static boolean stopRecaching = false;

    public static void register() {

        ServerWorldEvents.LOAD.register((minecraftServer, serverWorld) -> {
            stopRecaching = false;
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
            LOGGER.info("Clearing road cache...");
            roadDataMap.clear();
            RoadFeature.roadSegmentsCache.clear();
            RoadFeature.roadAttributesCache.clear();
            RoadFeature.roadChunksCache.clear();
        });
        ServerChunkEvents.CHUNK_GENERATE.register(ModEventHandler::clearRoad);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getWorlds().forEach(serverWorld -> {
                if (getRoadData(serverWorld) == null) {
                    return;
                };
                if (getRoadData(serverWorld).getStructureLocations().isEmpty()) {
                    return;
                };
                placeDecorations(serverWorld);
                updateSigns(serverWorld);
                if (ModConfig.loadRoadChunks){
                    loadRoadChunksCompletely(serverWorld);
                }
            });
        });
    }

    private static final ChunkTicketType<BlockPos> ROAD_TICKET = ChunkTicketType.create("road_ticket", Comparator.comparingLong(BlockPos::asLong));
    private static final Set<ChunkPos> toRemove = ConcurrentHashMap.newKeySet();
    private static final int MAX_BLOCKS_PER_TICK = 1;

    private static void loadRoadChunksCompletely(ServerWorld serverWorld) {
        if (!RoadFeature.roadChunksCache.isEmpty()) {
            stopRecaching = true;
            RoadFeature.roadChunksCache.removeIf(chunkPos -> serverWorld.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FEATURES, true) != null);
        }
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

    private static void updateSigns(ServerWorld serverWorld) {
        if (RoadFeature.signPostProcessingPositions.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, String>> iterator = RoadFeature.signPostProcessingPositions.iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos,String> signPostProcess = iterator.next();
            if (signPostProcess != null) {
                BlockPos signPos = signPostProcess.getKey();
                String text = signPostProcess.getValue();
                Chunk chunk = serverWorld.getChunk(new ChunkPos(signPos).x, new ChunkPos(signPos).z, ChunkStatus.FEATURES, false);
                if (chunk == null) {
                    continue;
                }
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
                    iterator.remove();
                }
            }
        }
    }

    private static void placeDecorations(ServerWorld serverWorld) {
        if (RoadFeature.roadDecorationPlacementPositions.isEmpty()) {
            return;
        }
        Iterator<Records.RoadDecoration> iterator = RoadFeature.roadDecorationPlacementPositions.iterator();
        while (iterator.hasNext()) {
            Records.RoadDecoration roadDecoration = iterator.next();
            if (roadDecoration != null) {
                BlockPos placePos = roadDecoration.placePos();
                Chunk chunk = serverWorld.getChunk(new ChunkPos(placePos).x, new ChunkPos(placePos).z, ChunkStatus.FEATURES, true);
                if (chunk == null) {
                    continue;
                }
                BlockPos surfacePos = placePos.withY(serverWorld.getChunk(placePos).sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, placePos.getX(), placePos.getZ())+1);
                BlockState blockStateBelow = serverWorld.getBlockState(surfacePos.down());
                if (blockStateBelow.isOf(Blocks.WATER) || blockStateBelow.isOf(Blocks.LAVA) || blockStateBelow.isIn(BlockTags.LOGS) || RoadFeature.dontPlaceHere.contains(blockStateBelow.getBlock())) {
                    iterator.remove();
                    continue;
                }
                int centerBlockCount = roadDecoration.centerBlockCount();
                String signText = roadDecoration.signText();
                Vec3i orthogonalVector = roadDecoration.vector();
                boolean isStart = roadDecoration.isStart();
                // place lantern
                if (centerBlockCount % 60 == 0){
                    RoadStructures.placeLantern(serverWorld, surfacePos, orthogonalVector, 1, true);
                }
                // place distance sign
                if (centerBlockCount == 10){
                    RoadStructures.placeDistanceSign(serverWorld, surfacePos, orthogonalVector, 1, true, signText);
                }
                if (!isStart) {
                    RoadStructures.placeDistanceSign(serverWorld, surfacePos, orthogonalVector, 1, false, signText);
                }
                iterator.remove();
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
