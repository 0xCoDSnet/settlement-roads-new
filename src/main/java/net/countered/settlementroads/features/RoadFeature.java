package net.countered.settlementroads.features;

import com.mojang.serialization.Codec;
import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.helpers.RoadMath;
import net.countered.settlementroads.persistence.RoadData;
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

    // Cache road paths per segment (start, end)
    public static final Map<Integer, Set<BlockPos>> roadBlocksCache = new HashMap<>();
    // Cache chunks where roads will be generated
    public static final Set<ChunkPos> roadChunksCache = new HashSet<>();

    private static Set<BlockState> dontPlaceHere = new HashSet<>();
    static {
        dontPlaceHere.add(Blocks.PACKED_ICE.getDefaultState());
        dontPlaceHere.add(Blocks.ICE.getDefaultState());
        dontPlaceHere.add(Blocks.BLUE_ICE.getDefaultState());
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
            runRoadLogic(currentChunk, structureWorldAccess, context);
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
            System.out.println(natural);
            // Calculate a determined path
            List<BlockPos> waypoints = RoadMath.generateControlPoints(start, end, deterministicRandom);
            Set<BlockPos> path = RoadMath.calculateSplinePath(waypoints, width, RoadMath.calculateDynamicSteps(start, end), natural, deterministicRandom);
            // Cache the path positions for the current road segment
            roadBlocksCache.put(roadId, path);
        }
    }

    private void runRoadLogic(ChunkPos currentChunk, StructureWorldAccess structureWorldAccess, FeatureContext<RoadFeatureConfig> context) {
        // Now use the cached path for block placement
        for (Map.Entry<Integer, Set<BlockPos>> entry : roadBlocksCache.entrySet()) {
            Random deterministicRandom = Random.create(entry.getKey());
            // Select material for road
            BlockState material = getRandomArtificialMaterial(deterministicRandom, context);
            int width = getRandomWidth(deterministicRandom, context);

            for (BlockPos pathPos : entry.getValue()) {
                ChunkPos pathChunk = new ChunkPos(pathPos);
                if (currentChunk.equals(pathChunk)) {
                    BlockPos placedPos = structureWorldAccess.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pathPos);
                    placeOnSurface(structureWorldAccess, placedPos, material, width);
                }
            }
        }
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

    private int placedInWaterCounter = 1;

    private void placeOnSurface(StructureWorldAccess structureWorldAccess, BlockPos placedPos, BlockState material, int width) {
        BlockState blockStateAtPos = structureWorldAccess.getBlockState(placedPos.down());
        if (blockStateAtPos.equals(Blocks.WATER.getDefaultState())) {
            // If it's water, place a buoy
            if (placedInWaterCounter % (35*width) == 0) {
                setBlockState(structureWorldAccess, placedPos.down(), Blocks.OAK_PLANKS.getDefaultState());
                setBlockState(structureWorldAccess, placedPos, Blocks.OAK_FENCE.getDefaultState());
                placedInWaterCounter = 1;
            }
            placedInWaterCounter++;
        } else {
            // If not water, just place the road
            if (!placeAllowedCheck(blockStateAtPos)) {
                return;
            }
            setBlockState(structureWorldAccess, placedPos.down(), material);
            setBlockState(structureWorldAccess, placedPos.up(0), Blocks.AIR.getDefaultState());
            setBlockState(structureWorldAccess, placedPos.up(1), Blocks.AIR.getDefaultState());
            setBlockState(structureWorldAccess, placedPos.up(2), Blocks.AIR.getDefaultState());
            setBlockState(structureWorldAccess, placedPos.up(3), Blocks.AIR.getDefaultState());
            placedInWaterCounter = 1;
        }
    }
    private boolean placeAllowedCheck (BlockState blockToCheck) {
        return !dontPlaceHere.contains(blockToCheck)
                && !blockToCheck.isIn(BlockTags.LEAVES)
                && !blockToCheck.isIn(BlockTags.UNDERWATER_BONEMEALS);
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
}

