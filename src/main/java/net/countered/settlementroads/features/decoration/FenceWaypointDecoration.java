package net.countered.settlementroads.features.decoration;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;

public class FenceWaypointDecoration extends Decoration{

    public FenceWaypointDecoration(BlockPos placePos, StructureWorldAccess world) {
        super(placePos, world);
    }

    @Override
    public void place() {
        if (!placeAllowed()) return;

        BlockPos surfacePos = this.getPos();
        StructureWorldAccess world = this.getWorld();

        world.setBlockState(surfacePos, Blocks.OAK_FENCE.getDefaultState(), 3);
        world.setBlockState(surfacePos.up(), Blocks.TORCH.getDefaultState(), 3);
    }
}
