package net.countered.settlementroads.events;


import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.helpers.StructureLocator;
import net.countered.settlementroads.persistence.RoadData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            updateSigns(server.getOverworld());
            clearRoad(server.getOverworld());
        });
    }

    private static final int MAX_BLOCKS_PER_TICK = 1;

    private static void clearRoad(ServerWorld serverWorld) {
        int processed = 0;
        while (!RoadFeature.roadPostProcessingPositions.isEmpty() && processed < MAX_BLOCKS_PER_TICK) {
            BlockPos roadBlockPos = RoadFeature.roadPostProcessingPositions.poll();
            if (roadBlockPos != null) {
                Block blockAbove = serverWorld.getBlockState(roadBlockPos.up()).getBlock();
                if (blockAbove == Blocks.SNOW) {
                    serverWorld.setBlockState(roadBlockPos.up(), Blocks.AIR.getDefaultState());
                }
            }
            processed++;
        }
    }

    private static void updateSigns(ServerWorld serverWorld) {
        int processed = 0;
        while (!RoadFeature.signPostProcessingPositions.isEmpty() && processed < MAX_BLOCKS_PER_TICK) {
            BlockPos signPos = RoadFeature.signPostProcessingPositions.poll();
            if (signPos != null) {
                BlockEntity entity = serverWorld.getBlockEntity(signPos);
                if (entity instanceof SignBlockEntity signEntity) {
                    signEntity.setText(new SignText().withMessage(1, Text.literal("ho")), true);
                    signEntity.markDirty();
                }
            }
            processed++;
        }
    }
}
