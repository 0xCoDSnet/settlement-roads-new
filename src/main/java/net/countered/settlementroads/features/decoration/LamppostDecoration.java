package net.countered.settlementroads.features.decoration;

import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructureWorldAccess;

public class LamppostDecoration extends OrientedDecoration {
    private final boolean leftRoadSide;

    public LamppostDecoration(BlockPos pos, Vec3i direction, StructureWorldAccess world, boolean leftRoadSide) {
        super(pos, direction, world);
        this.leftRoadSide = leftRoadSide;
    }

    @Override
    public void place() {
        if (!placeAllowed()) return;

        int rotation = getCardinalRotationFromVector(getOrthogonalVector(), leftRoadSide);
        DirectionProperties props = getDirectionProperties(rotation);

        BlockPos basePos = this.getPos();
        StructureWorldAccess world = this.getWorld();

        world.setBlockState(basePos.up(2).offset(props.offsetDirection.getOpposite()), Blocks.LANTERN.getDefaultState().with(Properties.HANGING, true), 3);
        placeFenceStructure(basePos, props);
    }
}
