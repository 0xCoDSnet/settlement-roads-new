package net.countered.settlementroads.features.roadlogic;

import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.features.config.RoadFeatureConfig;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.persistence.attachments.WorldDataAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Road {
    FeatureContext<RoadFeatureConfig> context;
    StructureWorldAccess worldAccess;

    public Road(StructureWorldAccess worldAccess, FeatureContext<RoadFeatureConfig> context) {
        this.worldAccess = worldAccess;
        this.context = context;
    }

    public void generateRoad(){
        Random random = Random.create();
        int width = getRandomWidth(random, context.getConfig());
        int type = allowedRoadTypes(random);
        // if all road types are disabled in config
        if (type == -1) {
            return;
        }
        List<BlockState> material = (type == 1) ? getRandomNaturalRoadMaterials(random, context.getConfig()) : getRandomArtificialRoadMaterials(random, context.getConfig());

        List<Records.VillageConnection> connectedVillages = worldAccess.toServerWorld().getAttached(WorldDataAttachment.CONNECTED_VILLAGES);
        BlockPos start = connectedVillages.get(connectedVillages.size()-1).from();
        BlockPos end = connectedVillages.get(connectedVillages.size()-1).to();

        List<BlockPos> waypoints = RoadPathingCalculator.generateControlPoints(start, end);
        Map<BlockPos, Set<BlockPos>> roadPath = RoadPathingCalculator.calculateSplinePath(waypoints, width);
        addRoadToChunks(roadPath, width, type, material, worldAccess);
    }

    private static int allowedRoadTypes(Random deterministicRandom) {
        if (ModConfig.allowArtificial && ModConfig.allowNatural){
            return getRandomRoadType(deterministicRandom);
        }
        else if (ModConfig.allowArtificial){
            return 0;
        }
        else if (ModConfig.allowNatural) {
            return 1;
        }
        else {
            return -1;
        }
    }

    private static int getRandomRoadType(Random random) {
        return random.nextBetween(0, 1);
    }

    private static List<BlockState> getRandomNaturalRoadMaterials(Random random, RoadFeatureConfig config) {
        List<List<BlockState>> materialsList = config.getNaturalMaterials();
        return materialsList.get(random.nextInt(materialsList.size()));
    }

    private static List<BlockState> getRandomArtificialRoadMaterials(Random random, RoadFeatureConfig config) {
        List<List<BlockState>> materialsList = config.getArtificialMaterials();
        return materialsList.get(random.nextInt(materialsList.size()));
    }

    private static int getRandomWidth(Random random, RoadFeatureConfig config) {
        List<Integer> widthList = config.getWidths();
        return widthList.get(random.nextInt(widthList.size()));
    }

    private void addRoadToChunks(Map<BlockPos, Set<BlockPos>> roadPath, int width, int type, List<BlockState> material, StructureWorldAccess worldAccess) {
        int segmentCounter = 0;

        for (Map.Entry<BlockPos, Set<BlockPos>> entry : roadPath.entrySet()) {
            BlockPos middlePos = entry.getKey();
            Set<BlockPos> blockPositions = entry.getValue();

            ChunkPos chunkPos = new ChunkPos(middlePos);
            int finalSegmentCounter = segmentCounter;

            Chunk chunk = worldAccess.toServerWorld().getChunk(chunkPos.x, chunkPos.z);
            List<Records.RoadData> roadDataList = chunk.getAttachedOrCreate(ChunkDataAttachment.ROAD_CHUNK_DATA_LIST, List::of);
            List<Records.RoadData> mutableList = new ArrayList<>(roadDataList);
            List<Records.RoadSegmentPlacement> placements = List.of(
                    new Records.RoadSegmentPlacement(finalSegmentCounter, middlePos, new ArrayList<>(blockPositions))
            );
            Records.RoadData data = new Records.RoadData(width, type, material, placements);
            mutableList.add(data);
            chunk.setAttached(ChunkDataAttachment.ROAD_CHUNK_DATA_LIST, mutableList);

            segmentCounter++;
        }
    }
}
