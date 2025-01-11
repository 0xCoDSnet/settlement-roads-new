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

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RoadFeature extends Feature<RoadFeatureConfig> {

    public static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

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

        if (locateStructure(300)){
            Helpers.locateStructures(serverWorld, 1);
            LOGGER.info(roadData.getStructureLocations().toString());
        }

        StructureWorldAccess structureWorldAccess = context.getWorld();
        BlockPos genPos = context.getOrigin();

        if (roadData.getStructureLocations().isEmpty()) {
            return false;
        }

        for (BlockPos pos : roadData.getStructureLocations()) {
            if (new ChunkPos(pos.getX(), pos.getZ()).equals(new ChunkPos(genPos.getX(), genPos.getZ()))) {
                setBlockState(structureWorldAccess, genPos.up(10), Blocks.DIAMOND_BLOCK.getDefaultState());
                LOGGER.info("Placed road at {}", genPos);
            }
        }
        return true;
    }

    private void generateRoad(WorldAccess worldAccess, BlockPos nearestVillage, BlockPos nearestVillageToFirst, FeatureContext<RoadFeatureConfig> context) {

        Random random = context.getRandom();

        List<BlockState> materialsList = context.getConfig().getMaterials();
        BlockState material = materialsList.get(random.nextInt(materialsList.size()));
        List<Integer> widthList = context.getConfig().getWidths();
        int width = widthList.get(random.nextInt(widthList.size()));
        List<Integer> qualityList = context.getConfig().getQualities();
        int quality = qualityList.get(random.nextInt(qualityList.size()));
        List<Integer> naturalList = context.getConfig().getNatural();
        int natural = naturalList.get(random.nextInt(naturalList.size()));

        for (int i = 0; i < 50; i++) {
            BlockPos.Mutable shiftedPos = nearestVillage.mutableCopy().set(nearestVillage.getX() + i, nearestVillage.getY(), nearestVillage.getZ());
            shiftedPos.setY(worldAccess.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, shiftedPos).getY());
            worldAccess.setBlockState(shiftedPos, material, 3);
            LOGGER.info("Placing block at {}", shiftedPos);
        }
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

