package net.countered.settlementroads.features;

import net.countered.settlementroads.SettlementRoads;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class ModConfiguredFeatures {

    public static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

    public static void bootstrap(Registerable<ConfiguredFeature<?,?>> context){
        LOGGER.info("Bootstrap ConfiguredFeature");
        context.register(RoadFeature.ROAD_FEATURE_KEY,
                new ConfiguredFeature<>(RoadFeature.ROAD_FEATURE,
                        new RoadFeatureConfig(
                                List.of(Blocks.DIRT_PATH.getDefaultState(), Blocks.MUD_BRICKS.getDefaultState(),
                                        Blocks.STONE_BRICKS.getDefaultState(), Blocks.COBBLESTONE.getDefaultState()),                                     // artificial
                                List.of(Blocks.COARSE_DIRT.getDefaultState(), Blocks.PACKED_MUD.getDefaultState(),
                                        Blocks.MOSSY_COBBLESTONE.getDefaultState(), Blocks.GRAVEL.getDefaultState()),                                     // natural
                                List.of(2,3,4),                                                                                                           // width
                                List.of(1,2,3,4,5,6,7,8,9)                                                                                                // quality
                        )
                )
        );
    }
}