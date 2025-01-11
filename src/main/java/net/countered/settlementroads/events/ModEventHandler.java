package net.countered.settlementroads.events;


import net.countered.settlementroads.helpers.Helpers;
import net.countered.settlementroads.persistence.RoadData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static net.countered.settlementroads.SettlementRoads.MOD_ID;

public class ModEventHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final int locateCount = 10;

    public static void register() {
        ServerWorldEvents.LOAD.register(ModEventHandler::onWorldLoaded);
    }

    private static void onWorldLoaded(MinecraftServer minecraftServer, ServerWorld serverWorld) {
        if (RoadData.getOrCreateRoadData(serverWorld).getStructureLocations().isEmpty()) {
            Helpers.locateStructures(serverWorld, 10);
        }
    }
}
