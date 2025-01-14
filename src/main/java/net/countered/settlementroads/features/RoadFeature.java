package net.countered.settlementroads.features;

import com.mojang.serialization.Codec;
import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.events.ModEventHandler;
import net.countered.settlementroads.helpers.Helpers;
import net.countered.settlementroads.persistence.RoadData;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RoadFeature extends Feature<RoadFeatureConfig> {

    public static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

    private static final Map<Integer, BlockState> roadMaterials = new HashMap<>();

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

        for (int i = 0; i < roadData.getStructureLocations().size() - 1; i++) {
            BlockPos start = roadData.getStructureLocations().get(i);
            BlockPos end = roadData.getStructureLocations().get(i + 1);

            int roadId = calculateRoadId(start, end);
            Random deterministicRandom = Random.create(roadId);

            // Deterministically select the material
            List<BlockState> materialsList = context.getConfig().getMaterials();
            BlockState material = materialsList.get(deterministicRandom.nextInt(materialsList.size()));

            // Deterministically select the width
            List<Integer> widthList = context.getConfig().getWidths();
            int width = widthList.get(deterministicRandom.nextInt(widthList.size()));

            List<BlockPos> path = calculateStraightPath(start, end, width);
            for (BlockPos pathPos : path) {
                ChunkPos pathChunk = new ChunkPos(pathPos);
                if (currentChunk.equals(pathChunk)) {
                    BlockPos placedPos = structureWorldAccess.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, pathPos);
                    setBlockState(structureWorldAccess, placedPos.down(), material);
                    for (int j = 0; i < 5; i++) {
                        if (structureWorldAccess.getBlockState(placedPos.up(i)).equals(Blocks.AIR.getDefaultState())) {
                            break;
                        }
                        setBlockState(structureWorldAccess, placedPos, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
    }

    private int calculateRoadId(BlockPos start, BlockPos end) {
        return start.hashCode() ^ end.hashCode();
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

    private List<BlockPos> calculateStraightPath(BlockPos start, BlockPos end, int width) {
        List<BlockPos> path = new ArrayList<>();
        int deltaX = end.getX() - start.getX();
        int deltaZ = end.getZ() - start.getZ();
        int steps = Math.max(Math.abs(deltaX), Math.abs(deltaZ));

        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            int x = (int) Math.round(start.getX() * (1 - t) + end.getX() * t);
            int z = (int) Math.round(start.getZ() * (1 - t) + end.getZ() * t);
            for (int w = (int) Math.round((double) -width / 2); w <= (int) Math.round((double) width / 2); w++) {
                path.add(new BlockPos(x + w, start.getY(), z));
            }
        }
        return path;
    }
}

