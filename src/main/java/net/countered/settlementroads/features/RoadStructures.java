package net.countered.settlementroads.features;

import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;

import java.util.Map;

public class RoadStructures {

    public static void placeBuoy(StructureWorldAccess worldAccess, BlockPos surfacePos) {
        worldAccess.setBlockState(surfacePos.down(), Blocks.OAK_PLANKS.getDefaultState(), 3);
        worldAccess.setBlockState(surfacePos, Blocks.OAK_FENCE.getDefaultState(), 3);
    }

    public static void placeDistanceSign(StructureWorldAccess worldAccess, BlockPos surfacePos, Vec3i orthogonalVector, int distance, boolean isStart, String signText) {

        BlockPos signPos = isStart ? surfacePos.add(orthogonalVector.multiply(distance)) : surfacePos.subtract(orthogonalVector.multiply(distance));

        int rotation = getCardinalRotationFromVector(orthogonalVector, isStart);

        BlockPos fixedYPos = signPos.withY(worldAccess.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, signPos.getX(), signPos.getZ()));

        Direction offsetDirection;
        Property<Boolean> reverseDirectionProperty;
        Property<Boolean> directionProperty;
        switch (rotation) {
            case 12  : offsetDirection = Direction.NORTH; reverseDirectionProperty = Properties.SOUTH; directionProperty = Properties.NORTH; break;
            case 0  : offsetDirection = Direction.EAST;  reverseDirectionProperty = Properties.WEST;  directionProperty = Properties.EAST; break;
            case 4  : offsetDirection = Direction.SOUTH; reverseDirectionProperty = Properties.NORTH; directionProperty = Properties.SOUTH; break;
            default : offsetDirection = Direction.WEST;  reverseDirectionProperty = Properties.EAST;  directionProperty = Properties.WEST; break;
        };

        worldAccess.setBlockState(fixedYPos.up(2), Blocks.OAK_HANGING_SIGN.getDefaultState().with(Properties.ROTATION, rotation).with(Properties.ATTACHED, true), 3);
        RoadFeature.signPostProcessingPositions.add(Map.entry(fixedYPos.up(2), signText));

        worldAccess.setBlockState(fixedYPos.up(3), Blocks.OAK_FENCE.getDefaultState().with(directionProperty, true), 3);
        worldAccess.setBlockState(fixedYPos.offset(offsetDirection).up(0), Blocks.OAK_FENCE.getDefaultState(), 3);
        worldAccess.setBlockState(fixedYPos.offset(offsetDirection).up(1), Blocks.OAK_FENCE.getDefaultState(), 3);
        worldAccess.setBlockState(fixedYPos.offset(offsetDirection).up(2), Blocks.OAK_FENCE.getDefaultState(), 3);
        worldAccess.setBlockState(fixedYPos.offset(offsetDirection).up(3), Blocks.OAK_FENCE.getDefaultState().with(reverseDirectionProperty, true), 3);

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
