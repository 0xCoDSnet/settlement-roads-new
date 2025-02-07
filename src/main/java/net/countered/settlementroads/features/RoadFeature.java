package net.countered.settlementroads.features;

import com.mojang.serialization.Codec;
import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.helpers.RoadMath;
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
    public static final Map<Integer, Map<BlockPos, Records.RoadSegmentData>> roadSegmentsCache = new HashMap<>();
    // Cache road attributes per roadId
    public static final Map<Integer, Records.RoadAttributesData> roadAttributesCache = new HashMap<>();
    // Cache chunks where roads will be generated
    public static final Set<ChunkPos> roadChunksCache = new HashSet<>();

    private static final Set<Block> dontPlaceHere = new HashSet<>();
    static {
        dontPlaceHere.add(Blocks.PACKED_ICE);
        dontPlaceHere.add(Blocks.ICE);
        dontPlaceHere.add(Blocks.BLUE_ICE);
        dontPlaceHere.add(Blocks.TALL_SEAGRASS);
    }

    private static int counter = 1;

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
        RoadData roadData = RoadData.getOrCreateRoadData(serverWorld);
        //RoadMath.estimateMemoryUsage();
        if (roadData.getStructureLocations().size() < 2) {
            return false;
        }
        generateRoad(roadData, context);
        return true;
    }

    private void generateRoad(RoadData roadData, FeatureContext<RoadFeatureConfig> context) {
        StructureWorldAccess structureWorldAccess = context.getWorld();
        BlockPos genPos = context.getOrigin();
        ChunkPos currentChunk = new ChunkPos(genPos);
        if (roadChunksCache.isEmpty()) {
            runCachingLogic(roadData, context);
        }
        if (roadChunksCache.contains(currentChunk)){
            runRoadLogic(currentChunk, structureWorldAccess);
        }
    }

    private void runCachingLogic(RoadData roadData, FeatureContext<RoadFeatureConfig> context) {
        List<BlockPos> villages = roadData.getStructureLocations();
        Map<BlockPos, BlockPos> closestVillageMap = new HashMap<>();

        for (BlockPos village : villages) {
            BlockPos closestVillage = findClosestVillage(village, villages);
            if (closestVillage != null) {
                closestVillageMap.put(village, closestVillage);
            }
        }
        // Generate roads for each village to its closest village
        for (Map.Entry<BlockPos, BlockPos> entry : closestVillageMap.entrySet()) {
            BlockPos start = entry.getKey();
            BlockPos end = entry.getValue();
            // Generate a unique road identifier for the current road segment
            int roadId = calculateRoadId(start, end);
            Random deterministicRandom = Random.create(roadId);
            int width = getRandomWidth(deterministicRandom, context);
            boolean natural = getRandomNatural(deterministicRandom);
            BlockState material = natural ? getRandomNaturalMaterial(deterministicRandom, context) : getRandomArtificialMaterial(deterministicRandom, context);

            // Calculate a determined path
            List<BlockPos> waypoints = RoadMath.generateControlPoints(start, end, deterministicRandom);
            Map<BlockPos, Records.RoadSegmentData> roadPath = RoadMath.calculateSplinePath(waypoints, width);

            roadAttributesCache.put(roadId, new Records.RoadAttributesData(width, natural, material, deterministicRandom));
            roadSegmentsCache.put(roadId, roadPath);
        }
    }

    private void runRoadLogic(ChunkPos currentChunk, StructureWorldAccess structureWorldAccess) {
        // Now use the cached path for block placement
        for (Map.Entry<Integer, Map<BlockPos, Records.RoadSegmentData>> roadEntry : roadSegmentsCache.entrySet()) {
            int roadId = roadEntry.getKey();
            Records.RoadAttributesData attributes = roadAttributesCache.get(roadId);
            BlockState material = attributes.material();
            boolean natural = attributes.natural();
            Random deterministicRandom = attributes.deterministicRandom();
            // Middle path placement with buoy logic
            int segmentIndex = 0;
            for (Map.Entry<BlockPos, Records.RoadSegmentData> segmentEntry : roadEntry.getValue().entrySet()) {
                Records.RoadSegmentData segment = segmentEntry.getValue();
                BlockPos middleBlock = segment.middle();
                ChunkPos middleChunk = new ChunkPos(middleBlock);

                // Place width blocks
                for (BlockPos widthBlock : segment.widthBlocks()) {
                    ChunkPos widthChunk = new ChunkPos(widthBlock);
                    if (!currentChunk.equals(widthChunk)) {
                        continue;
                    }
                    if (roadEntry.getValue().containsKey(widthBlock)) {
                        continue;
                    }
                    placeOnSurface(structureWorldAccess, widthBlock, material, natural, deterministicRandom, -1);
                }
                // Place middle block
                if (currentChunk.equals(middleChunk)) {
                    placeOnSurface(structureWorldAccess, middleBlock, material, natural, deterministicRandom, segmentIndex);
                }
                segmentIndex++;
            }
        }
    }


    private void placeOnSurface(StructureWorldAccess structureWorldAccess, BlockPos placePos, BlockState material, Boolean natural, Random deterministicRandom, int centerBlockCount) {
        double naturalBlockChance = 0.3;

        BlockPos surfacePos = structureWorldAccess.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, placePos);
        BlockState blockStateAtPos = structureWorldAccess.getBlockState(surfacePos.down());
        if (blockStateAtPos.equals(Blocks.WATER.getDefaultState())) {
            // If it's water, place a buoy
            if (centerBlockCount % (ModConfig.distanceBetweenBuoys+6) == 0) {
                setBlockState(structureWorldAccess, surfacePos.down(), Blocks.OAK_PLANKS.getDefaultState());
                setBlockState(structureWorldAccess, surfacePos, Blocks.OAK_FENCE.getDefaultState());
            }
        }
        else {
            if (!natural || deterministicRandom.nextDouble() < naturalBlockChance) {
                // If not water, just place the road
                if (!placeAllowedCheck(blockStateAtPos.getBlock())) {
                    return;
                }
                setBlockState(structureWorldAccess, surfacePos.down(), material);
                setBlockState(structureWorldAccess, surfacePos.up(0), Blocks.AIR.getDefaultState());
                setBlockState(structureWorldAccess, surfacePos.up(1), Blocks.AIR.getDefaultState());
                setBlockState(structureWorldAccess, surfacePos.up(2), Blocks.AIR.getDefaultState());
                setBlockState(structureWorldAccess, surfacePos.up(3), Blocks.AIR.getDefaultState());
            }
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

    private int calculateRoadId(BlockPos start, BlockPos end) {
        return start.hashCode() ^ end.hashCode();
    }

    private BlockPos findClosestVillage(BlockPos currentVillage, List<BlockPos> allVillages) {
        BlockPos closestVillage = null;
        double minDistance = Double.MAX_VALUE;

        for (BlockPos village : allVillages) {
            if (!village.equals(currentVillage)) {
                double distance = currentVillage.getSquaredDistance(village);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestVillage = village;
                }
            }
        }
        return closestVillage;
    }

    private Boolean locateStructure(int chunksNeeded) {
        if (counter % chunksNeeded != 0){
            counter++;
            return false;
        }
        LOGGER.info("Locating structure dynamically");
        counter = 1;
        return true;
    }
    private BlockState getRandomNaturalMaterial(Random deterministicRandom, FeatureContext<RoadFeatureConfig> context) {
        List<BlockState> materialsList = context.getConfig().getNaturalMaterials();
        return materialsList.get(deterministicRandom.nextInt(materialsList.size()));
    }

    private BlockState getRandomArtificialMaterial(Random deterministicRandom, FeatureContext<RoadFeatureConfig> context) {
        List<BlockState> materialsList = context.getConfig().getArtificialMaterials();
        return materialsList.get(deterministicRandom.nextInt(materialsList.size()));
    }

    private int getRandomWidth(Random deterministicRandom, FeatureContext<RoadFeatureConfig> context) {
        List<Integer> widthList = context.getConfig().getWidths();
        return widthList.get(deterministicRandom.nextInt(widthList.size()));
    }

    private boolean getRandomNatural(Random deterministicRandom) {
        return deterministicRandom.nextBoolean();
    }
}

