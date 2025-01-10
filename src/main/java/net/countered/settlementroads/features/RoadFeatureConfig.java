package net.countered.settlementroads.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.feature.FeatureConfig;

import java.util.List;

public class RoadFeatureConfig implements FeatureConfig {

    public final List<BlockState> materials;
    public final List<Integer> width;
    public final List<Integer> quality;
    public final List<Integer> natural;

    public static final Codec<RoadFeatureConfig> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                            BlockState.CODEC.listOf().fieldOf("materials").forGetter(RoadFeatureConfig::getMaterials),
                            Codec.INT.listOf().fieldOf("width").forGetter(RoadFeatureConfig::getWidths),
                            Codec.INT.listOf().fieldOf("quality").forGetter(RoadFeatureConfig::getQualities),
                            Codec.INT.listOf().fieldOf("natural").forGetter(RoadFeatureConfig::getNatural)
                    )
                    .apply(instance, RoadFeatureConfig::new));



    public RoadFeatureConfig(List<BlockState> materials, List<Integer> width, List<Integer> quality, List<Integer> natural) {
        this.materials = materials;
        this.width = width;
        this.quality = quality;
        this.natural = natural;
    }

    public List<BlockState> getMaterials() {
        return materials;
    }
    public List<Integer> getWidths() {
        return width;
    }
    public List<Integer> getQualities() {
        return quality;
    }
    public List<Integer> getNatural() {
        return natural;
    }
}
