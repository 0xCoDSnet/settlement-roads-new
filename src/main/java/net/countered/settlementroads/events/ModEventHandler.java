package net.countered.settlementroads.events;


import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.helpers.StructureLocator;
import net.countered.settlementroads.persistence.RoadData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.countered.settlementroads.SettlementRoads.MOD_ID;

public class ModEventHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static RoadData roadData;

    public static void register() {

        ServerWorldEvents.LOAD.register((minecraftServer, serverWorld) -> {
            if (!serverWorld.getRegistryKey().equals(World.OVERWORLD)) {
                return; // Only in Overworld
            }
            roadData = RoadData.getOrCreateRoadData(serverWorld);
            try {
                if (roadData.getStructureLocations().size() < ModConfig.initialLocatingCount) {
                    StructureLocator.locateConfiguredStructure(serverWorld, ModConfig.initialLocatingCount, false);
                }
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        });
        ServerWorldEvents.UNLOAD.register((minecraftServer, serverWorld) -> {
            if (!serverWorld.getRegistryKey().equals(World.OVERWORLD)) {
                return;
            }
            LOGGER.info("Clearing road cache...");
            RoadFeature.roadSegmentsCache.clear();
            RoadFeature.roadAttributesCache.clear();
            RoadFeature.roadChunksCache.clear();
        });
        // Example in a tick handler
        ServerTickEvents.END_WORLD_TICK.register(serverWorld -> {
            updateSigns(serverWorld);
        });
    }
    private static void updateSigns(ServerWorld serverWorld) {

        for (Records.RoadPostProcessingData roadPostProcessingData : RoadFeature.roadPostProcessingPositions) {
            for (BlockPos signPos : roadPostProcessingData.placedSignBlockPos()) {
                if (!serverWorld.getWorldChunk(signPos).getStatus().isAtLeast(ChunkStatus.FULL)) {
                    return;
                }
                // Update the sign
                BlockEntity entity = serverWorld.getBlockEntity(signPos);
                if (entity instanceof SignBlockEntity signEntity) {
                    signEntity.setText(new SignText().withMessage(1, Text.literal("ho")), true);
                    signEntity.markDirty();
                    LOGGER.info("Updated sign at " + signPos);
                }
            }
            //RoadFeature.roadPostProcessingPositions.remove(roadPostProcessingData);
        }
    }
}
