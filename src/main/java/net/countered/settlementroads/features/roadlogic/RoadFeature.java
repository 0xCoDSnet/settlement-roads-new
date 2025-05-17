package net.countered.settlementroads.features.roadlogic;

import com.mojang.serialization.Codec;
import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.features.config.RoadFeatureConfig;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.helpers.StructureConnector;
import net.countered.settlementroads.persistence.attachments.WorldDataAttachment;
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
import net.minecraft.util.math.Vec3i;
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
import java.util.concurrent.ConcurrentHashMap;

public class RoadFeature extends Feature<RoadFeatureConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

    // Road post-processing positions
    public static Set<BlockPos> roadPostProcessingPositions = ConcurrentHashMap.newKeySet();
    public static Set<Records.RoadDecoration> roadDecorationPlacementPositions = ConcurrentHashMap.newKeySet();

    public static final Set<Block> dontPlaceHere = new HashSet<>();
    static {
        dontPlaceHere.add(Blocks.PACKED_ICE);
        dontPlaceHere.add(Blocks.ICE);
        dontPlaceHere.add(Blocks.BLUE_ICE);
        dontPlaceHere.add(Blocks.TALL_SEAGRASS);
        dontPlaceHere.add(Blocks.MANGROVE_ROOTS);
    }

    public static int chunksForLocatingCounter = 1;

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
        StructureWorldAccess structureWorldAccess = context.getWorld();
        Records.StructureLocationData structureLocationData = serverWorld.getAttached(WorldDataAttachment.STRUCTURE_LOCATIONS);
        if (structureLocationData == null) {
            return false;
        }
        List<BlockPos> villageLocations = structureLocationData.structureLocations();
        tryFindNewVillageConnection(villageLocations, serverWorld);
        runRoadLogic(structureWorldAccess, context);
        RoadStructures.placeDecorations(structureWorldAccess, context);
        return true;
    }

    private void tryFindNewVillageConnection(List<BlockPos> villageLocations, ServerWorld serverWorld) {
        if (villageLocations == null || villageLocations.size() < ModConfig.maxLocatingCount) {
            chunksForLocatingCounter++;
            if (chunksForLocatingCounter > 300) {
                List<Records.VillageConnection> connectionList= serverWorld.getAttached(WorldDataAttachment.CONNECTED_VILLAGES);
                serverWorld.getServer().execute(() -> {
                    StructureConnector.cacheNewConnection(serverWorld, true);
                });
                chunksForLocatingCounter = 1;
            }
        }
    }

    private void runRoadLogic(StructureWorldAccess structureWorldAccess, FeatureContext<RoadFeatureConfig> context) {
        int averagingRadius = ModConfig.averagingRadius;
        List<Records.RoadData> roadDataList = structureWorldAccess.toServerWorld().getAttached(WorldDataAttachment.ROAD_DATA_LIST);
        if (roadDataList == null) return;

        ChunkPos currentChunkPos = new ChunkPos(context.getOrigin());

        for (Records.RoadData data : roadDataList) {
            int roadType = data.roadType();
            List<BlockState> materials = data.materials();
            List<Records.RoadSegmentPlacement> segmentList = data.roadSegmentList();

            List<BlockPos> middlePositions = segmentList.stream().map(Records.RoadSegmentPlacement::middlePos).toList();
            int segmentIndex = 0;
            for (int i = 2; i < segmentList.size() - 2; i++) {
                Records.RoadSegmentPlacement segment = segmentList.get(i);
                BlockPos currentMiddle = segment.middlePos();
                ChunkPos middleChunkPos = new ChunkPos(currentMiddle);
                segmentIndex++;
                if (!middleChunkPos.equals(currentChunkPos)) continue;

                BlockPos prevPos = middlePositions.get(i - 2);
                BlockPos nextPos = middlePositions.get(i + 2);
                List<Integer> heights = new ArrayList<>();
                for (int j = i - averagingRadius; j <= i + averagingRadius; j++) {
                    if (j >= 0 && j < middlePositions.size()) {
                        BlockPos samplePos = middlePositions.get(j);
                        int y = structureWorldAccess.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, samplePos.getX(), samplePos.getZ());
                        heights.add(y);
                    }
                }

                int averageY = (int) Math.round(heights.stream().mapToInt(Integer::intValue).average().orElse(currentMiddle.getY()));
                BlockPos averagedPos = new BlockPos(currentMiddle.getX(), averageY, currentMiddle.getZ());

                Random random = Random.create();

                for (BlockPos widthBlock : segment.positions()) {
                    BlockPos correctedYPos = new BlockPos(widthBlock.getX(), averageY, widthBlock.getZ());
                    placeOnSurface(structureWorldAccess, correctedYPos, materials, roadType, random, -1, nextPos, currentMiddle, middlePositions);
                }

                placeOnSurface(structureWorldAccess, averagedPos, materials, roadType, random, segmentIndex, nextPos, prevPos, middlePositions);
                addDecoration(structureWorldAccess, averagedPos, segmentIndex, nextPos, prevPos, middlePositions);

            }
        }
    }

    private void addDecoration(StructureWorldAccess structureWorldAccess, BlockPos placePos, int segmentIndex, BlockPos nextPos, BlockPos prevPos, List<BlockPos> middleBlockPositions) {
        if (!(segmentIndex == 10 || segmentIndex == middleBlockPositions.size()-10 || segmentIndex % 60 == 0)){
            return;
        }
        // Road vector
        Vec3i directionVector = new Vec3i(
                nextPos.getX() - prevPos.getX(),
                0,
                nextPos.getZ() - prevPos.getZ()
        );
        // orthogonal vector
        Vec3i orthogonalVector = new Vec3i(-directionVector.getZ(), 0, directionVector.getX());
        boolean isStart = segmentIndex != middleBlockPositions.size() - 10;
        BlockPos shiftedPos = isStart
                ? placePos.add(orthogonalVector.multiply(1))
                : placePos.subtract(orthogonalVector.multiply(1));

        roadDecorationPlacementPositions.add(new Records.RoadDecoration(shiftedPos, orthogonalVector, segmentIndex, String.valueOf(middleBlockPositions.size()), isStart));
    }

    private void placeOnSurface(StructureWorldAccess structureWorldAccess, BlockPos placePos, List<BlockState> material, int natural, Random deterministicRandom, int centerBlockCount, BlockPos nextPos, BlockPos prevPos, List<BlockPos> middleBlockPositions) {
        double naturalBlockChance = 0.5;
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
            if (ModConfig.placeWaypoints) {
                if (centerBlockCount % 30 == 0) {
                    RoadStructures.placeWaypointMarker(structureWorldAccess, surfacePos);
                }
                return;
            }
            // place road
            if (natural == 0 || deterministicRandom.nextDouble() < naturalBlockChance) {
                placeRoadBlock(structureWorldAccess, blockStateAtPos, surfacePos, material, deterministicRandom);
                // add road block position to post process
                roadPostProcessingPositions.add(surfacePos.down());
            }
        }
    }

    private void placeRoadBlock(StructureWorldAccess structureWorldAccess, BlockState blockStateAtPos, BlockPos surfacePos, List<BlockState> materials, Random deterministicRandom) {
        // If not water, just place the road
        if (!placeAllowedCheck(blockStateAtPos.getBlock())
                || (!structureWorldAccess.getBlockState(surfacePos.down()).isOpaque()
                && !structureWorldAccess.getBlockState(surfacePos.down(2)).isOpaque()
                && !structureWorldAccess.getBlockState(surfacePos.down(3)).isOpaque())
                || structureWorldAccess.getBlockState(surfacePos.up(3)).isOpaque()
        ){
            return;
        }
        BlockState material = materials.get(deterministicRandom.nextInt(materials.size()));
        setBlockState(structureWorldAccess, surfacePos.down(), material);

        for (int i = 0; i < 4; i++) {
            if (!structureWorldAccess.getBlockState(surfacePos.up(i)).getBlock().equals(Blocks.AIR)) {
                setBlockState(structureWorldAccess, surfacePos.up(i), Blocks.AIR.getDefaultState());
            }
            else {
                break;
            }
        }
        BlockPos belowPos1 = surfacePos.down(2);
        BlockPos belowPos2 = surfacePos.down(3);
        BlockPos belowPos3 = surfacePos.down(4);

        BlockState belowState1 = structureWorldAccess.getBlockState(belowPos1);
        BlockState belowState2 = structureWorldAccess.getBlockState(belowPos2);
        // fill dirt below
        if (structureWorldAccess.getBlockState(belowPos2).isOpaque() || structureWorldAccess.getBlockState(belowPos2).isOf(Blocks.GRASS_BLOCK)
                && !belowState1.isOpaque()) {
            setBlockState(structureWorldAccess, belowPos1, Blocks.DIRT.getDefaultState());
            setBlockState(structureWorldAccess, belowPos2, Blocks.DIRT.getDefaultState());
        }
        else if (structureWorldAccess.getBlockState(belowPos3).isOpaque() || structureWorldAccess.getBlockState(belowPos3).isOf(Blocks.GRASS_BLOCK)
                && !belowState1.isOpaque()
                && !belowState2.isOpaque()
        ) {
            setBlockState(structureWorldAccess, belowPos1, Blocks.DIRT.getDefaultState());
            setBlockState(structureWorldAccess, belowPos2, Blocks.DIRT.getDefaultState());
            setBlockState(structureWorldAccess, belowPos3, Blocks.DIRT.getDefaultState());
        }
    }

    private boolean placeAllowedCheck (Block blockToCheck) {
        return !(dontPlaceHere.contains(blockToCheck)
                || blockToCheck.getDefaultState().isIn(BlockTags.LEAVES)
                || blockToCheck.getDefaultState().isIn(BlockTags.LOGS)
                || blockToCheck.getDefaultState().isIn(BlockTags.UNDERWATER_BONEMEALS)
        );
    }
}

