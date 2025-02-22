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

public class RoadFeature extends Feature<RoadFeatureConfig> {

    public static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

    // Cache road paths per segment roadId
    public static Map<Integer, Set<Records.RoadSegmentData>> roadSegmentsCache = new LinkedHashMap<>();
    // Cache road attributes per roadId
    public static Map<Integer, Records.RoadAttributesData> roadAttributesCache = new HashMap<>();
    // Cache chunks where roads will be generated
    public static final Set<ChunkPos> roadChunksCache = new HashSet<>();
    // Villages that need to be added to cache
    public static Set<BlockPos> pendingVillagesToCache = new HashSet<>();
    // Road post-processing positions
    public static List<BlockPos> roadPostProcessingPositions = new ArrayList<>();
    public static List<BlockPos> signPostProcessingPositions = new ArrayList<>();

    private static final Set<Block> dontPlaceHere = new HashSet<>();
    static {
        dontPlaceHere.add(Blocks.PACKED_ICE);
        dontPlaceHere.add(Blocks.ICE);
        dontPlaceHere.add(Blocks.BLUE_ICE);
        dontPlaceHere.add(Blocks.TALL_SEAGRASS);
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
        // Now use the cached path for block placement
        for (Map.Entry<Integer, Set<Records.RoadSegmentData>> roadEntry : roadSegmentsCache.entrySet()) {
            int roadId = roadEntry.getKey();
            Records.RoadAttributesData attributes = roadAttributesCache.get(roadId);
            BlockState material = attributes.material();
            int natural = attributes.natural();
            Random deterministicRandom = attributes.deterministicRandom();
            // Middle path placement with buoy logic
            int segmentIndex = 0;
            for (Records.RoadSegmentData segmentEntry : roadEntry.getValue()) {
                segmentIndex++;
                if (segmentIndex == 1){
                    continue;
                }
                BlockPos middleBlockPos = segmentEntry.middle();
                ChunkPos middleChunk = new ChunkPos(middleBlockPos);

                // Place width blocks
                for (BlockPos widthBlockPos : segmentEntry.widthBlocks()) {
                    ChunkPos widthChunk = new ChunkPos(widthBlockPos);
                    if (!currentChunk.equals(widthChunk)) {
                        continue;
                    }
                    placeOnSurface(structureWorldAccess, widthBlockPos, material, natural, deterministicRandom, -1);
                }
                // Place middle block
                if (currentChunk.equals(middleChunk)) {
                    placeOnSurface(structureWorldAccess, middleBlockPos, material, natural, deterministicRandom, segmentIndex);
                }
            }
        }
    }

    private void placeOnSurface(StructureWorldAccess structureWorldAccess, BlockPos placePos, BlockState material, int natural, Random deterministicRandom, int centerBlockCount) {
        double naturalBlockChance = 0.3;
        BlockPos surfacePos = structureWorldAccess.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, placePos);
        BlockState blockStateAtPos = structureWorldAccess.getBlockState(surfacePos.down());
        if (blockStateAtPos.equals(Blocks.WATER.getDefaultState())) {
            // If it's water, place a buoy
            if (centerBlockCount % (ModConfig.distanceBetweenBuoys + 6) == 0) {
                setBlockState(structureWorldAccess, surfacePos.down(), Blocks.OAK_PLANKS.getDefaultState());
                setBlockState(structureWorldAccess, surfacePos, Blocks.OAK_FENCE.getDefaultState());
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
                if (!placeAllowedCheck(blockStateAtPos.getBlock())) {
                    return;
                }
                setBlockState(structureWorldAccess, surfacePos.down(), material);
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

