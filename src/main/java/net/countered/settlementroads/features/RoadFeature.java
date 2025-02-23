package net.countered.settlementroads.features;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.events.ModEventHandler;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.helpers.StructureLocator;
import net.countered.settlementroads.persistence.RoadData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RoadFeature extends Feature<RoadFeatureConfig> {

    public static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

    // Cache road paths per segment roadId
    public static Map<Integer, Map<BlockPos, Set<BlockPos>>> roadSegmentsCache = new LinkedHashMap<>();
    // Cache road attributes per roadId
    public static Map<Integer, Records.RoadAttributesData> roadAttributesCache = new HashMap<>();
    // Cache chunks where roads will be generated
    public static final Set<ChunkPos> roadChunksCache = new HashSet<>();
    // Villages that need to be added to cache
    public static Set<BlockPos> pendingVillagesToCache = new HashSet<>();
    // Road post-processing positions
    public static Queue<BlockPos> roadPostProcessingPositions = new ConcurrentLinkedQueue<>();
    public static Queue<BlockPos> signPostProcessingPositions = new ConcurrentLinkedQueue<>();

    private static final Set<Block> dontPlaceHere = new HashSet<>();
    static {
        dontPlaceHere.add(Blocks.PACKED_ICE);
        dontPlaceHere.add(Blocks.ICE);
        dontPlaceHere.add(Blocks.BLUE_ICE);
        dontPlaceHere.add(Blocks.TALL_SEAGRASS);
        dontPlaceHere.add(Blocks.MANGROVE_ROOTS);
    }

    private static int chunksForLocatingCounter = 1;

    public static final RegistryKey<PlacedFeature> ROAD_FEATURE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(SettlementRoads.MOD_ID, "road_feature_placed"));
    public static final RegistryKey<ConfiguredFeature<?,?>> ROAD_FEATURE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(SettlementRoads.MOD_ID, "road_feature"));
    public static final Feature<RoadFeatureConfig> ROAD_FEATURE = new RoadFeature(RoadFeatureConfig.CODEC);
    public RoadFeature(Codec<RoadFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<RoadFeatureConfig> context) {
        ServerWorld serverWorld = context.getWorld().toServerWorld();
        RoadData roadData = ModEventHandler.roadData;
        //RoadMath.estimateMemoryUsage();

        if (roadData.getStructureLocations().size() < 2) {
            return false;
        }
        if (roadData.getStructureLocations().size() < ModConfig.maxLocatingCount) {
            locateStructureDynamically(serverWorld, 400);
        }

        RoadCaching.cacheDynamicVillages(roadData, context);

        generateRoad(roadData, context);
        return true;
    }

    private void generateRoad(RoadData roadData, FeatureContext<RoadFeatureConfig> context) {
        StructureWorldAccess structureWorldAccess = context.getWorld();
        BlockPos genPos = context.getOrigin();
        ChunkPos currentChunk = new ChunkPos(genPos);
        if (roadChunksCache.isEmpty()) {
            RoadCaching.runCachingLogic(roadData, context);
        }
        if (roadChunksCache.contains(currentChunk)){
            runRoadLogic(currentChunk, structureWorldAccess);
        }
    }

    private void runRoadLogic(ChunkPos currentChunk, StructureWorldAccess structureWorldAccess) {
        int averagingRadius = 4;

        for (Map.Entry<Integer, Map<BlockPos, Set<BlockPos>>> roadEntry : roadSegmentsCache.entrySet()) {
            int roadId = roadEntry.getKey();
            Records.RoadAttributesData attributes = roadAttributesCache.get(roadId);
            BlockState material = attributes.material();
            int natural = attributes.natural();
            Random deterministicRandom = attributes.deterministicRandom();

            int segmentIndex = 0;
            List<BlockPos> middleBlockPositions = new ArrayList<>(roadEntry.getValue().keySet());

            for (BlockPos middleBlockPos : middleBlockPositions) {
                segmentIndex++;
                if (segmentIndex == 1) continue;

                ChunkPos middleChunk = new ChunkPos(middleBlockPos);

                if (currentChunk.equals(middleChunk)) {

                    List<Integer> heights = new ArrayList<>();

                    for (int i = -averagingRadius; i <= averagingRadius; i++) {
                        int index = segmentIndex - 1 + i;
                        if (index >= 0 && index < middleBlockPositions.size()) {
                            BlockPos neighborPos = middleBlockPositions.get(index);
                            BlockPos surfacePos = structureWorldAccess.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, neighborPos);
                            heights.add(surfacePos.getY());
                        }
                    }

                    int averageY = (int) Math.round(heights.stream().mapToInt(Integer::intValue).average().orElse(middleBlockPos.getY()));
                    BlockPos averagedPos = new BlockPos(middleBlockPos.getX(), averageY, middleBlockPos.getZ());

                    placeOnSurface(structureWorldAccess, averagedPos, material, natural, deterministicRandom, segmentIndex);

                    for (BlockPos widthBlockPos : roadEntry.getValue().get(middleBlockPos)) {
                        BlockPos correctedYPos = new BlockPos(widthBlockPos.getX(), averageY, widthBlockPos.getZ());
                        placeOnSurface(structureWorldAccess, correctedYPos, material, natural, deterministicRandom, -1);
                    }
                }
            }
        }
    }


    private void placeOnSurface(StructureWorldAccess structureWorldAccess, BlockPos placePos, BlockState material, int natural, Random deterministicRandom, int centerBlockCount) {
        double naturalBlockChance = 0.3;
        BlockPos surfacePos = placePos;
        if (natural == 1) {
            surfacePos = structureWorldAccess.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, placePos);
        }
        BlockState blockStateAtPos = structureWorldAccess.getBlockState(structureWorldAccess.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, surfacePos).down());
        if (blockStateAtPos.equals(Blocks.WATER.getDefaultState())) {
            // If it's water, place a buoy
            if (centerBlockCount % (ModConfig.distanceBetweenBuoys + 6) == 0) {
                RoadStructures.placeBuoy(structureWorldAccess, surfacePos);
            }
        }
        else {
            if (centerBlockCount % 20 == 0 && structureWorldAccess.getBlockState(surfacePos.down()).isOpaqueFullCube()){
                signPostProcessingPositions.add(surfacePos);
                RoadStructures.placeDistanceSign(structureWorldAccess, surfacePos);
            }
            if (ModConfig.placeWaypoints) {
                if (centerBlockCount % 30 == 0) {
                    RoadStructures.placeWaypointMarker(structureWorldAccess, surfacePos);
                }
                return;
            }
            // place road
            if (natural == 0 || deterministicRandom.nextDouble() < naturalBlockChance) {
                // If not water, just place the road
                if (!placeAllowedCheck(blockStateAtPos.getBlock())
                        || (!structureWorldAccess.getBlockState(surfacePos.down()).isOpaque()
                        && !structureWorldAccess.getBlockState(surfacePos.down(2)).isOpaque()
                        && !structureWorldAccess.getBlockState(surfacePos.down(3)).isOpaque())
                        || structureWorldAccess.getBlockState(surfacePos.up(3)).isOpaque()
                ){
                    return;
                }
                setBlockState(structureWorldAccess, surfacePos.down(), material);

                for (int i = 0; i < 4; i++) {
                    if (i >= 2 && !structureWorldAccess.getBlockState(surfacePos.down(i)).isOpaque()
                            || structureWorldAccess.getBlockState(surfacePos.down(i)).isOf(Blocks.GRASS_BLOCK)) {
                        setBlockState(structureWorldAccess, surfacePos.down(i), Blocks.DIRT.getDefaultState());
                    }
                    if (!structureWorldAccess.getBlockState(surfacePos.up(i)).getBlock().equals(Blocks.AIR)) {
                        setBlockState(structureWorldAccess, surfacePos.up(i), Blocks.AIR.getDefaultState());
                    }
                }
            }
            // add road block position to post process
            roadPostProcessingPositions.add(surfacePos.down());
        }
    }

    private int placeBridge(Map.Entry<BlockPos, Integer> blockPosEntry, StructureWorldAccess structureWorldAccess) {
        BlockPos placePos = blockPosEntry.getKey();
        BlockPos surfacePos = structureWorldAccess.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, placePos);
        BlockState blockStateAtPos = structureWorldAccess.getBlockState(surfacePos.down());
        if (blockStateAtPos.isOf(Blocks.WATER)) {
            setBlockState(structureWorldAccess, surfacePos, Blocks.OAK_PLANKS.getDefaultState());
            return 0;
        }
        return -1;
    }

    private boolean placeAllowedCheck (Block blockToCheck) {
        return !(dontPlaceHere.contains(blockToCheck)
                || blockToCheck.getDefaultState().isIn(BlockTags.LEAVES)
                || blockToCheck.getDefaultState().isIn(BlockTags.LOGS)
                || blockToCheck.getDefaultState().isIn(BlockTags.UNDERWATER_BONEMEALS)
        );
    }

    private void locateStructureDynamically(ServerWorld serverWorld, int chunksNeeded) {
        if (chunksForLocatingCounter % chunksNeeded != 0){
            chunksForLocatingCounter++;
        }
        else {
            LOGGER.info("Locating structure dynamically");
            try {
                StructureLocator.locateConfiguredStructure(serverWorld, 1, true);
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
            chunksForLocatingCounter = 1;
        }
    }
}

