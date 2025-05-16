package net.countered.settlementroads.features.roadlogic;

import com.mojang.serialization.Codec;
import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.features.config.RoadFeatureConfig;
import net.countered.settlementroads.helpers.Records;
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

    public static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

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
        //System.out.println("villagelocationsize " +villageLocations.size() );
        if (villageLocations == null || villageLocations.size() < ModConfig.maxLocatingCount) {
            chunksForLocatingCounter++;
            if (chunksForLocatingCounter > 300) {
                List<Records.VillageConnection> connectionList= serverWorld.getAttached(WorldDataAttachment.CONNECTED_VILLAGES);
                if (connectionList != null) {
                    System.out.println(connectionList.size());
                }
                serverWorld.getServer().execute(() -> {
                    StructureConnector.generateNewConnections(serverWorld);
                    new Road(structureWorldAccess, context).generateRoad();
                } );
                chunksForLocatingCounter = 1;
            }
        }
        generateRoad(structureWorldAccess, villageLocations, context);
        RoadStructures.placeDecorations(structureWorldAccess, context);
        return true;
    }

    private void generateRoad(StructureWorldAccess structureWorldAccess, List<BlockPos> villageLocations, FeatureContext<RoadFeatureConfig> context) {
        BlockPos genPos = context.getOrigin();
        ChunkPos currentChunkPos = new ChunkPos(genPos);
        List<Records.RoadData> roadChunkData = structureWorldAccess.getChunk(genPos).getAttached(ChunkDataAttachment.ROAD_CHUNK_DATA_LIST);
        if (roadChunkData == null) {
            return;
        }
        System.out.println("generating abc 2");
        runRoadLogic(currentChunkPos, structureWorldAccess, roadChunkData);
    }

    private void runRoadLogic(ChunkPos currentChunkPos, StructureWorldAccess structureWorldAccess, List<Records.RoadData> roadChunkDataList) {
        int averagingRadius = ModConfig.averagingRadius;

        for (Records.RoadData data : roadChunkDataList) {
            int width = data.width();
            int roadType = data.roadType();
            List<BlockState> materials = data.materials();
            List<Records.RoadSegmentPlacement> segmentList = data.roadSegmentList();

            for (int i = 0; i < segmentList.size()-1; i++) {
                Records.RoadSegmentPlacement segment = segmentList.get(i);

                List<BlockPos> widthPositions = segment.positions();

                int segmentIndex = segment.segmentIndex();
                BlockPos middlePos = segment.middlePos();

                ChunkPos middleChunkPos = new ChunkPos(middlePos);

                if (currentChunkPos.equals(middleChunkPos)) {

                    List<Integer> heights = new ArrayList<>();
                    for (int j = -averagingRadius; j <= averagingRadius; j++) {
                        BlockPos samplePos = middlePos.add(j, 0, j);
                        int y = structureWorldAccess.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, samplePos.getX(), samplePos.getZ());
                        heights.add(y);
                    }

                    int averageY = (int) heights.stream().mapToInt(a -> a).average().orElse(middlePos.getY());
                    BlockPos averagedPos = new BlockPos(middlePos.getX(), averageY, middlePos.getZ());

                    for (BlockPos widthBlock : widthPositions) {
                        BlockPos correctedYPos = new BlockPos(widthBlock.getX(), averageY, widthBlock.getZ());
                        placeOnSurface(structureWorldAccess, correctedYPos, materials, roadType, Random.create(), -1, null, null, null);
                    }

                    placeOnSurface(structureWorldAccess, averagedPos, materials, roadType, Random.create(), segmentIndex, null, null, null);
                    addDecoration(structureWorldAccess, averagedPos, segmentIndex, null, null, null);
                }
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
}

