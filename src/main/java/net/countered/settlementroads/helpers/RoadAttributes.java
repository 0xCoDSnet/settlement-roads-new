package net.countered.settlementroads.helpers;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.random.Random;

public record RoadAttributes(int width, boolean natural, BlockState material, Random deterministicRandom) {
}

