package net.countered.settlementroads.events;


import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.helpers.StructureLocator;
import net.countered.settlementroads.persistence.RoadData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
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

import java.util.ArrayList;
import java.util.List;

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

    private static void clearRoad(ServerWorld serverWorld) {
        List<BlockPos> toProcess = new ArrayList<>(RoadFeature.roadPostProcessingPositions);

        for (BlockPos roadBlockPos : toProcess) {
            for (int i = 1; i < 4; i++) {
                if (serverWorld.getBlockState(roadBlockPos.up(i)).getBlock() == Blocks.AIR) {
                    break;
                }
                serverWorld.setBlockState(roadBlockPos.up(i), Blocks.AIR.getDefaultState());
            }
            RoadFeature.roadPostProcessingPositions.remove(roadBlockPos);
        }
    }

    private static void updateSigns(ServerWorld serverWorld) {
        List<BlockPos> toProcess = new ArrayList<>(RoadFeature.signPostProcessingPositions);

        for (BlockPos signPos : toProcess) {

            BlockEntity entity = serverWorld.getBlockEntity(signPos);
            if (entity instanceof SignBlockEntity signEntity) {
                signEntity.setText(new SignText().withMessage(1, Text.literal("ho")), true);
                signEntity.markDirty();
                LOGGER.info("Updated sign at " + signPos);
            }
            RoadFeature.signPostProcessingPositions.remove(signPos);
        }
    }
}
