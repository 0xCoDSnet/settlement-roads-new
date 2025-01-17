package net.countered.settlementroads.features;

import com.mojang.serialization.Codec;
import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.helpers.RoadMath;
import net.countered.settlementroads.persistence.RoadData;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
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

    private static int counter = 1;
    private static int placedBlocksCounter = 1;

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
        if (!roadChunksCache.isEmpty()) {
            if (!roadChunksCache.contains(currentChunk)){
                return;
            }
        }

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
            List<BlockState> materialsList = context.getConfig().getMaterials();
            BlockState material = materialsList.get(deterministicRandom.nextInt(materialsList.size()));
            List<Integer> widthList = context.getConfig().getWidths();
            int width = widthList.get(deterministicRandom.nextInt(widthList.size()));

            // If the road path is already cached, use it
            Set<BlockPos> cachedPath = roadBlocksCache.get(roadId);
            if (cachedPath == null) {
                // If not cached, generate and cache the road path
                List<BlockPos> waypoints = RoadMath.generateControlPoints(start, end, deterministicRandom);
                Set<BlockPos> path = RoadMath.calculateSplinePath(waypoints, width, RoadMath.calculateDynamicSteps(start, end));

                // Cache the path positions for the current road segment
                roadBlocksCache.put(roadId, path);

                // Use the generated path directly for the current chunk
                cachedPath = path;
            }


            // Now use the cached path for block placement
            for (BlockPos pathPos : cachedPath) {
                ChunkPos pathChunk = new ChunkPos(pathPos);
                if (currentChunk.equals(pathChunk)) {
                    BlockPos placedPos = structureWorldAccess.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, pathPos);
                    placeOnSurface(structureWorldAccess, placedPos, material);
                }
            }
        }
    }

    private void placeOnSurface(StructureWorldAccess structureWorldAccess, BlockPos placedPos, BlockState material) {
        // Check if the block is water
        BlockState blockStateAtPos = structureWorldAccess.getBlockState(placedPos.down());
        if (blockStateAtPos.getBlock() == Blocks.WATER) {
            // If it's water, place a buoy every 50 blocks
            if (placedBlocksCounter % 100 == 0) {
                // Place the buoy
                setBlockState(structureWorldAccess, placedPos.down(), Blocks.BARREL.getDefaultState());
            }
            placedBlocksCounter++;  // Increment the counter for the next 50 blocks
        } else {
            // If not water, just place the road
            setBlockState(structureWorldAccess, placedPos.down(), material);
            placedBlocksCounter = 1;
        }
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

