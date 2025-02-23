package net.countered.settlementroads.features;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;

public class RoadStructures {

    public static void placeBuoy(StructureWorldAccess worldAccess, BlockPos surfacePos) {
        worldAccess.setBlockState(surfacePos.down(), Blocks.OAK_PLANKS.getDefaultState(), 3);
        worldAccess.setBlockState(surfacePos, Blocks.OAK_FENCE.getDefaultState(), 3);
    }

    public static void placeDistanceSign(StructureWorldAccess worldAccess, BlockPos pos) {
        BlockState signState = Blocks.OAK_SIGN.getDefaultState();
        worldAccess.setBlockState(pos, signState, 3);
    }

    public static void placeWaypointMarker(StructureWorldAccess worldAccess, BlockPos pos) {
        worldAccess.setBlockState(pos, Blocks.COBBLESTONE_WALL.getDefaultState(), 3);
        worldAccess.setBlockState(pos.up(), Blocks.TORCH.getDefaultState(), 3);
    }

    public static void placeLantern(StructureWorldAccess worldAccess ){}
}
