package net.countered.settlementroads.features.roadlogic;

import net.countered.settlementroads.helpers.Records;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RoadPathCalculator {

    private final static int neighborDistance = 4;

    // Cache for height values, mapping hashed (x, z) to height (y)
    public static final Map<Long, Integer> heightCache = new ConcurrentHashMap<>();

    // Helper method to hash (x, зеро зет) coordinates into a single long
    private static long hashXZ(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    // Height sampler method
    private static int heightSampler(int x, int z, ServerWorld serverWorld) {
        long key = hashXZ(x, z);
        return heightCache.computeIfAbsent(key, k -> serverWorld.getChunkManager()
                .getChunkGenerator()
                .getHeightInGround(x, z, Heightmap.Type.WORLD_SURFACE_WG, serverWorld, serverWorld.getChunkManager().getNoiseConfig()));
    }

    // Biome sampler method
    private static RegistryEntry<Biome> biomeSampler(BlockPos pos, ServerWorld serverWorld) {
        return serverWorld.getBiome(pos);
    }

    public static List<Records.RoadSegmentPlacement> calculateAStarRoadPath(
            BlockPos start, BlockPos end, int width, ServerWorld serverWorld
    ) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, List<BlockPos>> interpolatedSegments = new HashMap<>();

        BlockPos startGround = new BlockPos(start.getX(), heightSampler(start.getX(), start.getZ(), serverWorld), start.getZ());
        BlockPos endGround = new BlockPos(end.getX(), heightSampler(end.getX(), end.getZ(), serverWorld), end.getZ());

        Node startNode = new Node(startGround, null, 0.0, heuristic(startGround, endGround));
        openSet.add(startNode);
        allNodes.put(startGround, startNode);

        int maxSteps = 3000;

        int d = neighborDistance;
        int[][] neighborOffsets = {
                {d, 0}, {-d, 0}, {0, d}, {0, -d},
                {d, d}, {d, -d}, {-d, d}, {-d, -d}
        };

        while (!openSet.isEmpty() && maxSteps-- > 0) {
            System.out.println(maxSteps);
            Node current = openSet.poll();

            if (current.pos.withY(0).getManhattanDistance(endGround.withY(0)) < neighborDistance * 2) {
                System.out.println("Found path!");
                return reconstructPath(current, width, interpolatedSegments);
            }

            closedSet.add(current.pos);

            for (int[] offset : neighborOffsets) {
                BlockPos neighborXZ = current.pos.add(offset[0], 0, offset[1]);
                int y = heightSampler(neighborXZ.getX(), neighborXZ.getZ(), serverWorld);
                BlockPos neighborPos = new BlockPos(neighborXZ.getX(), y, neighborXZ.getZ());

                if (closedSet.contains(neighborPos)) continue;

                RegistryEntry<Biome> biomeRegistryEntry = biomeSampler(neighborPos, serverWorld);
                int biomeCost = biomeRegistryEntry.isIn(BiomeTags.IS_RIVER) || biomeRegistryEntry.isIn(BiomeTags.IS_OCEAN) || biomeRegistryEntry.isIn(BiomeTags.IS_DEEP_OCEAN) ? 500 : 0;
                double elevation = Math.abs(y - current.pos.getY());
                if (elevation > 3) {
                    continue;
                }
                int offsetSum = Math.abs(offset[0]) + Math.abs(offset[1]);
                double stepCost = (offsetSum == 2 * neighborDistance) ? neighborDistance * 1.414 : neighborDistance;

                double tentativeG = current.gScore + stepCost + elevation * 30 + biomeCost;

                Node neighbor = allNodes.get(neighborPos);
                if (neighbor == null || tentativeG < neighbor.gScore) {
                    double h = heuristic(neighborPos, endGround);
                    neighbor = new Node(neighborPos, current, tentativeG, tentativeG + h);
                    allNodes.put(neighborPos, neighbor);
                    openSet.add(neighbor);

                    List<BlockPos> segmentPoints = new ArrayList<>();
                    for (int i = 1; i < neighborDistance; i++) {
                        int interpX = current.pos.getX() + (offset[0] * i) / neighborDistance;
                        int interpZ = current.pos.getZ() + (offset[1] * i) / neighborDistance;
                        BlockPos interpolated = new BlockPos(interpX, current.pos.getY(), interpZ);
                        segmentPoints.add(interpolated);
                    }
                    interpolatedSegments.put(neighborPos, segmentPoints);
                }
            }
        }
        return Collections.emptyList();
    }

    private static List<Records.RoadSegmentPlacement> reconstructPath(
            Node endNode, int width, Map<BlockPos, List<BlockPos>> interpolatedPathMap
    ) {
        List<Node> pathNodes = new ArrayList<>();
        Node current = endNode;
        while (current != null) {
            pathNodes.add(current);
            current = current.parent;
        }
        Collections.reverse(pathNodes);

        Map<BlockPos, Set<BlockPos>> roadSegments = new LinkedHashMap<>();
        Set<BlockPos> widthCache = new HashSet<>();

        for (Node node : pathNodes) {
            BlockPos pos = node.pos;

            List<BlockPos> interpolated = interpolatedPathMap.getOrDefault(pos, Collections.emptyList());
            for (BlockPos interp : interpolated) {
                Set<BlockPos> widthSetInterp = generateWidth(interp, width / 2, widthCache);
                roadSegments.put(interp, widthSetInterp);
            }

            Set<BlockPos> widthSet = generateWidth(pos, width / 2, widthCache);
            roadSegments.put(pos, widthSet);
        }

        List<Records.RoadSegmentPlacement> result = new ArrayList<>();
        for (Map.Entry<BlockPos, Set<BlockPos>> entry : roadSegments.entrySet()) {
            result.add(new Records.RoadSegmentPlacement(entry.getKey(), new ArrayList<>(entry.getValue())));
        }
        return result;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dz = a.getZ() - b.getZ();
        int dy = Math.abs(a.getY() - b.getY());
        double dxzApprox = Math.abs(dx) + Math.abs(dz) - 0.6 * Math.min(Math.abs(dx), Math.abs(dz));
        return dxzApprox * 100;
    }

    private static class Node {
        BlockPos pos;
        Node parent;
        double gScore, fScore;

        Node(BlockPos pos, Node parent, double gScore, double fScore) {
            this.pos = pos;
            this.parent = parent;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }

    private static Set<BlockPos> generateWidth(BlockPos center, int radius, Set<BlockPos> widthPositionsCache) {
        Set<BlockPos> segmentWidthPositions = new HashSet<>();

        int centerX = center.getX();
        int centerZ = center.getZ();
        int y = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = new BlockPos(centerX + dx, y, centerZ + dz);
                if (!widthPositionsCache.contains(pos)) {
                    widthPositionsCache.add(pos);
                    segmentWidthPositions.add(pos);
                }
            }
        }
        return segmentWidthPositions;
    }
}
