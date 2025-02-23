package net.countered.settlementroads.helpers;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.random.Random;

public class Records {

    public record RoadAttributesData(int width, int natural, BlockState material, Random deterministicRandom) {}
}
