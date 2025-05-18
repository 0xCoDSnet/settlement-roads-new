package net.countered.settlementroads.helpers;

import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.persistence.attachments.WorldDataAttachment;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class StructureConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);
    public static Queue<Records.VillageConnection> cachedVillageConnections = new ArrayDeque<>();
    
    public static void cacheNewConnection(ServerWorld serverWorld, boolean locateAtPlayer) {
        StructureLocator.locateConfiguredStructure(serverWorld, 1, locateAtPlayer);
        List<BlockPos> villagePosList = serverWorld.getAttached(WorldDataAttachment.STRUCTURE_LOCATIONS).structureLocations();
        if (villagePosList == null || villagePosList.size() < 2) {
            return;
        }
        createNewStructureConnection(serverWorld);
    }

    private static void createNewStructureConnection(ServerWorld serverWorld) {
        System.out.println(("creating new structure connection"));
        List<BlockPos> villagePosList = serverWorld.getAttached(WorldDataAttachment.STRUCTURE_LOCATIONS).structureLocations();
        BlockPos latestVillagePos = villagePosList.get(villagePosList.size() - 1);
        Records.StructureLocationData structureLocationData = serverWorld.getAttached(WorldDataAttachment.STRUCTURE_LOCATIONS);
        List<BlockPos> worldStructureLocations = structureLocationData.structureLocations();

        BlockPos closestVillage = findClosestVillage(latestVillagePos, worldStructureLocations);

        if (closestVillage != null) {
            List<Records.VillageConnection> connections = new ArrayList<>(
                    serverWorld.getAttachedOrCreate(WorldDataAttachment.CONNECTED_VILLAGES, ArrayList::new)
            );
            if (!connectionExists(connections, latestVillagePos, closestVillage)) {
                Records.VillageConnection villageConnection = new Records.VillageConnection(latestVillagePos, closestVillage);
                connections.add(villageConnection);
                serverWorld.setAttached(WorldDataAttachment.CONNECTED_VILLAGES, connections);
                cachedVillageConnections.add(villageConnection);
            }
        }
    }

    private static boolean connectionExists(List<Records.VillageConnection> existingConnections, BlockPos a, BlockPos b) {
        for (Records.VillageConnection connection : existingConnections) {
            if ((connection.from().equals(a) && connection.to().equals(b)) ||
                    (connection.to().equals(b) && connection.from().equals(a))) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos findClosestVillage(BlockPos currentVillage, List<BlockPos> allVillages) {
        BlockPos closestVillage = null;
        double minDistance = Double.MAX_VALUE;

        for (BlockPos village : allVillages) {
            if (!village.equals(currentVillage)) {
                double distance = currentVillage.getSquaredDistance(village);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestVillage = village;
                }
            }
        }
        return closestVillage;
    }
}
