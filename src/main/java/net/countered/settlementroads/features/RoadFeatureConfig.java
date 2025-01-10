package net.countered.settlementroads.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.FeatureConfig;

import java.util.List;

public class RoadFeatureConfig implements FeatureConfig {

    public static final Codec<RoadFeatureConfig> CODEC = Codec.unit(new RoadFeatureConfig());

    public RoadFeatureConfig() {
    }
}
