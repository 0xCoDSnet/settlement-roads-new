package net.countered.settlementroads.persistence;

import com.mojang.serialization.Codec;
import net.countered.settlementroads.SettlementRoads;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ChunkRoadDataAttachment {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);


    public static final AttachmentType<List<BlockPos>> BOT_SLAB_POSITIONS = AttachmentRegistry.createPersistent(
            Identifier.of(SettlementRoads.MOD_ID, "bot_slab_positions"),
            Codec.list(BlockPos.CODEC)
    );

    public static final AttachmentType<List<BlockPos>> TOP_SLAB_POSITIONS = AttachmentRegistry.createPersistent(
            Identifier.of(SettlementRoads.MOD_ID, "top_slab_positions"),
            Codec.list(BlockPos.CODEC)
    );

    public static void registerChunkRoadDataAttachment() {
        LOGGER.info("Registering ChunkRoadData attachment");
    }
}
