package net.countered.settlementroads.features;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;

public class RoadStructures {
    public static void placeDistanceSign(StructureWorldAccess world, BlockPos pos) {
        BlockState signState = Blocks.OAK_SIGN.getDefaultState();
        world.setBlockState(pos, signState, 3);
    }

    public static void placeMileMarker(StructureWorldAccess worldAccess ){

    }
    public static void placeLantern(StructureWorldAccess worldAccess ){}
}
