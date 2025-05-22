package net.countered.settlementroads.features.decoration;

import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructureWorldAccess;

public abstract class OrientedDecoration extends Decoration{

    private final Vec3i orthogonalVector;

    public OrientedDecoration(BlockPos placePos, Vec3i orthogonalVector, StructureWorldAccess world) {
        super(placePos, world);
        this.orthogonalVector = orthogonalVector;
    }

    protected final int getCardinalRotationFromVector(Vec3i orthogonalVector, boolean start) {
        if (start) {
            if (Math.abs(orthogonalVector.getX()) > Math.abs(orthogonalVector.getZ())) {
                return orthogonalVector.getX() > 0 ? 0 : 8; // N or S
            } else {
                return orthogonalVector.getZ() > 0 ? 4 : 12; // E or W
            }
        }
        else {
            if (Math.abs(orthogonalVector.getX()) > Math.abs(orthogonalVector.getZ())) {
                return orthogonalVector.getX() > 0 ? 8 : 0; // N or S
            } else {
                return orthogonalVector.getZ() > 0 ? 12 : 4; // E or W
            }
        }
    }

    public Vec3i getOrthogonalVector() {
        return orthogonalVector;
    }

    protected static class DirectionProperties {
        Direction offsetDirection;
        Property<Boolean> reverseDirectionProperty;
        Property<Boolean> directionProperty;

        DirectionProperties(Direction offset, Property<Boolean> reverse, Property<Boolean> direction) {
            this.offsetDirection = offset;
            this.reverseDirectionProperty = reverse;
            this.directionProperty = direction;
        }
    }

    protected DirectionProperties getDirectionProperties(int rotation) {
        return switch (rotation) {
            case 12 -> new DirectionProperties(Direction.NORTH, Properties.SOUTH, Properties.NORTH);
            case 0 -> new DirectionProperties(Direction.EAST,  Properties.WEST,  Properties.EAST);
            case 4 -> new DirectionProperties(Direction.SOUTH, Properties.NORTH, Properties.SOUTH);
            default -> new DirectionProperties(Direction.WEST,  Properties.EAST,  Properties.WEST);
        };
    }

    protected void placeFenceStructure(BlockPos pos, DirectionProperties props) {
        StructureWorldAccess world = this.getWorld();

        // Hanging Lantern oder Sign bereits vorher setzen!
        world.setBlockState(pos.up(3).offset(props.offsetDirection.getOpposite()), Blocks.SPRUCE_FENCE.getDefaultState().with(props.directionProperty, true), 3);
        world.setBlockState(pos.up(0), Blocks.SPRUCE_FENCE.getDefaultState(), 3);
        world.setBlockState(pos.up(1), Blocks.SPRUCE_FENCE.getDefaultState(), 3);
        world.setBlockState(pos.up(2), Blocks.SPRUCE_FENCE.getDefaultState(), 3);
        world.setBlockState(pos.up(3), Blocks.SPRUCE_FENCE.getDefaultState().with(props.reverseDirectionProperty, true), 3);
    }
}
