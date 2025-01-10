package net.countered.settlementroads.features;

import com.mojang.serialization.Codec;
import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.persistence.RoadData;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RoadFeature extends Feature<RoadFeatureConfig> {

    public static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

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
        WorldAccess worldAccess = context.getWorld();
        BlockPos.Mutable posMutable = context.getOrigin().mutableCopy();
        Random random = context.getRandom();

        List<BlockState> materialsList = context.getConfig().getMaterials();
        BlockState material = materialsList.get(random.nextInt(materialsList.size()));
        List<Integer> widthList = context.getConfig().getWidths();
        int width = widthList.get(random.nextInt(widthList.size()));
        List<Integer> qualityList = context.getConfig().getQualities();
        int quality = qualityList.get(random.nextInt(qualityList.size()));
        List<Integer> naturalList = context.getConfig().getNatural();
        int natural = naturalList.get(random.nextInt(naturalList.size()));

        posMutable.set(posMutable.getX(), worldAccess.getTopY(Heightmap.Type.WORLD_SURFACE_WG, posMutable.getX(), posMutable.getZ()), posMutable.getZ());

        if (RoadData.getOrCreateRoadData(context.getWorld().toServerWorld()).getStructureLocations().isEmpty()){
            return false;
        }
        setBlockState(worldAccess, posMutable, material);
        return true;
    }
}

