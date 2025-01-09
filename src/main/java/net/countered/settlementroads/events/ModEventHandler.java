package net.countered.settlementroads.events;

import net.countered.settlementroads.villagelocation.VillageLocator;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.datafixer.fix.StructureFeatureChildrenPoolElementFix;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;

public class ModEventHandler {

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(ModEventHandler::onServerStarted);
    }

    private static void onServerStarted(MinecraftServer server) {
        VillageLocator villageLocator = new VillageLocator(10);  // Locate 5 villages

        for (int i = 0; i < 10; i++) {
            villageLocator.locateVillagesAsync(() -> {
                // Logic to locate a village
                ServerWorld serverWorld = server.getOverworld();

                BlockPos villagePos = serverWorld.locateStructure(
                        StructureTags.VILLAGE,
                        serverWorld.getSpawnPos(),
                        100,
                        true
                );

                System.out.println("Village located at "+villagePos);
            });
        }
    }
}
