package net.countered.settlementroads.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.feature.FeatureConfig;

import java.util.List;

public class RoadFeatureConfig implements FeatureConfig {

    public final List<BlockState> artificialMaterials;
    public final List<BlockState> naturalMaterials;
    public final List<Integer> width;
    public final List<Integer> quality;

    public static final Codec<RoadFeatureConfig> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                            BlockState.CODEC.listOf().fieldOf("artificialMaterials").forGetter(RoadFeatureConfig::getArtificialMaterials),
                            BlockState.CODEC.listOf().fieldOf("naturalMaterials").forGetter(RoadFeatureConfig::getNaturalMaterials),
                            Codec.INT.listOf().fieldOf("width").forGetter(RoadFeatureConfig::getWidths),
                            Codec.INT.listOf().fieldOf("quality").forGetter(RoadFeatureConfig::getQualities)
                    )
                    .apply(instance, RoadFeatureConfig::new));



    public RoadFeatureConfig(List<BlockState> artificialMaterials, List<BlockState> naturalMaterials, List<Integer> width, List<Integer> quality) {
        this.artificialMaterials = artificialMaterials;
        this.naturalMaterials = naturalMaterials;
        this.width = width;
        this.quality = quality;
    }

    public List<BlockState> getArtificialMaterials() {
        return artificialMaterials;
    }
    public List<BlockState> getNaturalMaterials() {
        return naturalMaterials;
    }
    public List<Integer> getWidths() {
        return width;
    }
    public List<Integer> getQualities() {
        return quality;
    }
}
