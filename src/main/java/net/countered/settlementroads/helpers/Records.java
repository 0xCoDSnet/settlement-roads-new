package net.countered.settlementroads.helpers;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.Set;

public class Records {
    public record RoadAttributesData(int width, boolean natural, BlockState material, Random deterministicRandom) {
    }

    public record RoadSegmentData (BlockPos middle, Set<BlockPos> widthBlocks){
    }
}
