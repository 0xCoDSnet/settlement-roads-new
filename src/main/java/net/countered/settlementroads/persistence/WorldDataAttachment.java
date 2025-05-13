package net.countered.settlementroads.persistence;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.countered.settlementroads.SettlementRoads;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class WorldDataAttachment {
    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

    public static final AttachmentType<List<Pair<BlockPos, BlockPos>>> CONNECTED_VILLAGES = AttachmentRegistry.createPersistent(
            new Identifier(SettlementRoads.MOD_ID, "connected_villages"),
            Codec.list(Codec.pair(BlockPos.CODEC, BlockPos.CODEC))
    );

    public static final AttachmentType<VillageLocationData> VILLAGE_LOCATIONS = AttachmentRegistry.createPersistent(
            new Identifier(SettlementRoads.MOD_ID, "village_locations"),
            VillageLocationData.CODEC
    );

    public static void registerWorldDataAttachment() {
        LOGGER.info("Registering WorldData attachment");
    }
}
