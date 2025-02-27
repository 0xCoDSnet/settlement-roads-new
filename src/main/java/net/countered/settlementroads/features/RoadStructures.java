package net.countered.settlementroads.features;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructureWorldAccess;

import java.util.Map;

public class RoadStructures {

    public enum DecorationType {
        SIGN, LANTERN
    }

    public static void placeBuoy(StructureWorldAccess worldAccess, BlockPos surfacePos) {
        worldAccess.setBlockState(surfacePos.down(), Blocks.SPRUCE_PLANKS.getDefaultState(), 3);
        worldAccess.setBlockState(surfacePos, Blocks.SPRUCE_FENCE.getDefaultState(), 3);
    }

    public static void placeDecoration(
            ServerWorld serverWorld,
            BlockPos surfacePos,
            Vec3i orthogonalVector,
            int distance,
            boolean isStart,
            DecorationType type,
            String signText
    ) {
        int rotation = getCardinalRotationFromVector(orthogonalVector, isStart);

        Direction offsetDirection;
        Property<Boolean> reverseDirectionProperty;
        Property<Boolean> directionProperty;
        switch (rotation) {
            case 12 -> { offsetDirection = Direction.NORTH; reverseDirectionProperty = Properties.SOUTH; directionProperty = Properties.NORTH; }
            case 0 -> { offsetDirection = Direction.EAST;  reverseDirectionProperty = Properties.WEST;  directionProperty = Properties.EAST; }
            case 4 -> { offsetDirection = Direction.SOUTH; reverseDirectionProperty = Properties.NORTH; directionProperty = Properties.SOUTH; }
            default -> { offsetDirection = Direction.WEST;  reverseDirectionProperty = Properties.EAST;  directionProperty = Properties.WEST; }
        }

        if (type == DecorationType.SIGN) {
            serverWorld.setBlockState(surfacePos.up(2).offset(offsetDirection.getOpposite()), Blocks.SPRUCE_HANGING_SIGN.getDefaultState()
                    .with(Properties.ROTATION, rotation)
                    .with(Properties.ATTACHED, true), 3 );
            RoadFeature.signPostProcessingPositions.add(Map.entry(surfacePos.up(2).offset(offsetDirection.getOpposite()), signText));

        } else if (type == DecorationType.LANTERN) {
            serverWorld.setBlockState(surfacePos.up(2).offset(offsetDirection.getOpposite()), Blocks.LANTERN.getDefaultState()
                    .with(Properties.HANGING, true), 3);
        }

        serverWorld.setBlockState(surfacePos.up(3).offset(offsetDirection.getOpposite()), Blocks.SPRUCE_FENCE.getDefaultState().with(directionProperty, true), 3);
        serverWorld.setBlockState(surfacePos.up(0), Blocks.SPRUCE_FENCE.getDefaultState(),3);
        serverWorld.setBlockState(surfacePos.up(1), Blocks.SPRUCE_FENCE.getDefaultState(),3);
        serverWorld.setBlockState(surfacePos.up(2), Blocks.SPRUCE_FENCE.getDefaultState(),3);
        serverWorld.setBlockState(surfacePos.up(3), Blocks.SPRUCE_FENCE.getDefaultState().with(reverseDirectionProperty, true), 3);
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

    public static void placeDistanceSign(
            ServerWorld serverWorld,
            BlockPos surfacePos,
            Vec3i orthogonalVector,
            int distance,
            boolean isStart,
            String signText
    ) {
        placeDecoration(serverWorld, surfacePos, orthogonalVector, distance, isStart, DecorationType.SIGN, signText);
    }

    public static void placeLantern(
            ServerWorld serverWorld,
            BlockPos surfacePos,
            Vec3i orthogonalVector,
            int distance,
            boolean isStart
    ) {
        placeDecoration(serverWorld, surfacePos, orthogonalVector, distance, isStart, DecorationType.LANTERN, null);
    }

    public static void placeWaypointMarker(StructureWorldAccess worldAccess, BlockPos surfacePos) {
        worldAccess.setBlockState(surfacePos, Blocks.COBBLESTONE_WALL.getDefaultState(), 3);
        worldAccess.setBlockState(surfacePos.up(), Blocks.TORCH.getDefaultState(), 3);
    }

}
