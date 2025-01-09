package net.countered.settlementroads.events;


import net.countered.settlementroads.persistence.RoadData;
import net.countered.settlementroads.villagelocation.StructureLocator;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static net.countered.settlementroads.SettlementRoads.MOD_ID;

public class ModEventHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(ModEventHandler::onServerStarted);
    }

    private static void onServerStarted(MinecraftServer server) {
        ServerWorld serverWorld = server.getOverworld();
        RoadData roadData = RoadData.getOrCreateRoadData(serverWorld);
        StructureLocator villageLocator = new StructureLocator(roadData, serverWorld);
        villageLocator.locateVillagesAsync(10);
    }
}
