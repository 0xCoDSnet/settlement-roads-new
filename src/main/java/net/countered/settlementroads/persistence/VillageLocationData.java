package net.countered.settlementroads.persistence;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class VillageLocationData {
    private final List<BlockPos> villages;

    public VillageLocationData(List<BlockPos> villages) {
        this.villages = new ArrayList<>(villages); // mutable copy
    }

    public List<BlockPos> getVillages() {
        return villages;
    }

    public void addVillage(BlockPos pos) {
        villages.add(pos);
    }

    public static final Codec<VillageLocationData> CODEC = BlockPos.CODEC
            .listOf()
            .xmap(VillageLocationData::new, VillageLocationData::getVillages);
}

