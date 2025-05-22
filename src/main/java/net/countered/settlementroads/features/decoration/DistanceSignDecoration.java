package net.countered.settlementroads.features.decoration;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HangingSignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructureWorldAccess;

import java.util.Objects;

public class DistanceSignDecoration extends OrientedDecoration {
    private final boolean isStart;
    private final String signText;

    public DistanceSignDecoration(BlockPos pos, Vec3i direction, StructureWorldAccess world, Boolean isStart, String distanceText) {
        super(pos, direction, world);
        this.isStart = isStart;
        this.signText = distanceText;
    }

    @Override
    public void place() {
        if (!placeAllowed()) return;

        int rotation = getCardinalRotationFromVector(getOrthogonalVector(), isStart);
        DirectionProperties props = getDirectionProperties(rotation);

        BlockPos basePos = this.getPos();
        StructureWorldAccess world = this.getWorld();

        BlockPos signPos = basePos.up(2).offset(props.offsetDirection.getOpposite());
        world.setBlockState(signPos, Blocks.SPRUCE_HANGING_SIGN.getDefaultState()
                .with(Properties.ROTATION, rotation)
                .with(Properties.ATTACHED, true), 3);
        updateSigns(world, signPos, signText);

        placeFenceStructure(basePos, props);
    }

    private void updateSigns(StructureWorldAccess structureWorldAccess, BlockPos surfacePos, String text) {
        Objects.requireNonNull(structureWorldAccess.getServer()).execute( () -> {
            BlockEntity signEntity = structureWorldAccess.getBlockEntity(surfacePos);
            if (signEntity instanceof HangingSignBlockEntity signBlockEntity) {
                signBlockEntity.setWorld(structureWorldAccess.toServerWorld());
                SignText signText = signBlockEntity.getText(true);
                signText = (signText.withMessage(0, Text.literal("----------")));
                signText = (signText.withMessage(1, Text.literal("Next Village")));
                signText = (signText.withMessage(2, Text.literal(text + "m")));
                signText = (signText.withMessage(3, Text.literal("----------")));
                signBlockEntity.setText(signText, true);

                SignText signTextBack = signBlockEntity.getText(false);
                signTextBack = signTextBack.withMessage(0, Text.of("----------"));
                signTextBack = signTextBack.withMessage(1, Text.of("Welcome"));
                signTextBack = signTextBack.withMessage(2, Text.of("traveller"));
                signTextBack = signTextBack.withMessage(3, Text.of("----------"));
                signBlockEntity.setText(signTextBack, false);

                signBlockEntity.markDirty();
            }
        });
    }
}
