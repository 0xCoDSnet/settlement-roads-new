package net.countered.settlementroads.helpers;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.Set;

public class Records {

    public record RoadAttributesData(int width, int natural, BlockState material, Random deterministicRandom) {}

    public record RoadSegmentData (BlockPos middle, Set<BlockPos> widthBlocks){}

    public record RoadPostProcessingData (BlockPos placedRoadBlockPos, BlockPos placedSignBlockPos){}
}
