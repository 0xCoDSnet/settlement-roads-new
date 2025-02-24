package net.countered.settlementroads.features;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;

public class RoadStructures {

    public static void placeBuoy(StructureWorldAccess worldAccess, BlockPos surfacePos) {
        worldAccess.setBlockState(surfacePos.down(), Blocks.OAK_PLANKS.getDefaultState(), 3);
        worldAccess.setBlockState(surfacePos, Blocks.OAK_FENCE.getDefaultState(), 3);
    }

    public static void placeDistanceSign(StructureWorldAccess worldAccess, BlockPos surfacePos, Vec3i orthogonalVector, int distance, boolean start) {

        BlockPos signPos = surfacePos.add(orthogonalVector.multiply(distance));

        int rotation = getCardinalRotationFromVector(orthogonalVector, start);

        BlockState signState = Blocks.OAK_SIGN.getDefaultState().with(Properties.ROTATION, rotation);

        BlockPos fixedYPos = signPos.withY(worldAccess.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, signPos.getX(), signPos.getZ()));
        worldAccess.setBlockState(fixedYPos, signState, 3);
        RoadFeature.signPostProcessingPositions.add(fixedYPos);
    }

    private static int getCardinalRotationFromVector(Vec3i vector, boolean start) {
        if (start) {
            if (Math.abs(vector.getX()) > Math.abs(vector.getZ())) {
                return vector.getX() > 0 ? 0 : 8; // N or S
            } else {
                return vector.getZ() > 0 ? 4 : 12; // E or W
            }
        }
        else {
            if (Math.abs(vector.getX()) > Math.abs(vector.getZ())) {
                return vector.getX() > 0 ? 8 : 0; // N or S
            } else {
                return vector.getZ() > 0 ? 12 : 4; // E or W
            }
        }
    }

    public static void placeWaypointMarker(StructureWorldAccess worldAccess, BlockPos surfacePos) {
        worldAccess.setBlockState(surfacePos, Blocks.COBBLESTONE_WALL.getDefaultState(), 3);
        worldAccess.setBlockState(surfacePos.up(), Blocks.TORCH.getDefaultState(), 3);
    }

    public static void placeLantern(StructureWorldAccess worldAccess, BlockPos surfacePos, Vec3i vector, int distance) {
        worldAccess.setBlockState(surfacePos, Blocks.LANTERN.getDefaultState(), 3);
    }
}
