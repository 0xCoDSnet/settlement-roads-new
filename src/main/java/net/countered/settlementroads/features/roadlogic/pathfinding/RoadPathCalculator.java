package net.countered.settlementroads.features.roadlogic.pathfinding;

import net.countered.settlementroads.helpers.Records;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class RoadPathCalculator {

    private final static int neighborDistance = 3;

    public static List<Records.RoadSegmentPlacement> calculateAStarRoadPath(
            BlockPos start, BlockPos end, int width, HeightmapSampler heightSampler
    ) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, List<BlockPos>> interpolatedSegments = new HashMap<>();

        BlockPos startGround = new BlockPos(start.getX(), heightSampler.sample(start.getX(), start.getZ()), start.getZ());
        BlockPos endGround = new BlockPos(end.getX(), heightSampler.sample(end.getX(), end.getZ()), end.getZ());

        Node startNode = new Node(startGround, null, 0.0, heuristic(startGround, endGround));
        openSet.add(startNode);
        allNodes.put(startGround, startNode);

        int maxSteps = 10000;

        int d = neighborDistance;
        int[][] neighborOffsets = {
                { d,  0}, {-d,  0}, { 0,  d}, { 0, -d},
                { d,  d}, { d, -d}, {-d,  d}, {-d, -d}
        };

        while (!openSet.isEmpty() && maxSteps-- > 0) {
            System.out.println(maxSteps);
            Node current = openSet.poll();

            if (current.pos.withY(0).getManhattanDistance(endGround.withY(0)) < neighborDistance * 2) {
                System.out.println("found path");
                return reconstructPath(current, width, interpolatedSegments);
            }

            closedSet.add(current.pos);

            for (int[] offset : neighborOffsets) {
                BlockPos neighborXZ = current.pos.add(offset[0], 0, offset[1]);
                int y = heightSampler.sample(neighborXZ.getX(), neighborXZ.getZ());
                BlockPos neighborPos = new BlockPos(neighborXZ.getX(), y, neighborXZ.getZ());

                if (closedSet.contains(neighborPos)) continue;

                double elevation = Math.abs(y - current.pos.getY());
                double stepCost = (Math.abs(offset[0]) + Math.abs(offset[1]) == 2 * neighborDistance) ?
                        neighborDistance * Math.sqrt(2) : neighborDistance;

                double tentativeG = current.gScore + stepCost + elevation * 5;

                Node neighbor = allNodes.get(neighborPos);
                if (neighbor == null || tentativeG < neighbor.gScore) {
                    double h = heuristic(neighborPos, endGround);
                    neighbor = new Node(neighborPos, current, tentativeG, tentativeG + h);
                    allNodes.put(neighborPos, neighbor);
                    openSet.add(neighbor);

                    // Interpolation der Zwischenpunkte von current -> neighbor (auf gleicher Y-Höhe)
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
        System.out.println("no suitable path found");
        return Collections.emptyList();
    }

    private static List<Records.RoadSegmentPlacement> reconstructPath(
            Node endNode, int width, Map<BlockPos, List<BlockPos>> interpolatedPathMap
    ) {
        Map<BlockPos, Set<BlockPos>> roadSegments = new LinkedHashMap<>();
        Set<BlockPos> widthCache = new HashSet<>();
        Node current = endNode;

        while (current != null) {
            BlockPos pos = current.pos;

            // Segment an Haupt-Position
            Set<BlockPos> widthSet = generateWidth(pos, width / 2, widthCache);
            roadSegments.put(pos, widthSet);

            // Segment an interpolierten Zwischenpositionen
            List<BlockPos> interpolated = interpolatedPathMap.getOrDefault(pos, Collections.emptyList());
            for (BlockPos interp : interpolated) {
                Set<BlockPos> widthSetInterp = generateWidth(interp, width / 2, widthCache);
                roadSegments.put(interp, widthSetInterp);
            }

            current = current.parent;
        }

        List<Records.RoadSegmentPlacement> result = new ArrayList<>();
        for (Map.Entry<BlockPos, Set<BlockPos>> entry : roadSegments.entrySet()) {
            result.add(new Records.RoadSegmentPlacement(entry.getKey(), new ArrayList<>(entry.getValue())));
        }
        return result;
    }


    private static double heuristic(BlockPos a, BlockPos b) {
        double dxzDistance = Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getZ() - b.getZ(), 2));
        double elevationDifference = Math.abs(a.getY() - b.getY());
        return dxzDistance * 3 + elevationDifference;
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

        // Wir erzeugen einen Kreis (Radius in Blöcken) um center.
        // Die Y-Höhe wird von center übernommen.
        int rSquared = radius * radius;
        int centerX = center.getX();
        int centerZ = center.getZ();
        int y = center.getY();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                //if (dx * dx + dz * dz <= rSquared) {
                    BlockPos pos = new BlockPos(centerX + dx, y, centerZ + dz);
                    //if (!widthPositionsCache.contains(pos)) {
                        widthPositionsCache.add(pos);
                        segmentWidthPositions.add(pos);
                    //}
                //}
            }
        }
        return segmentWidthPositions;
    }

    public interface HeightmapSampler {
        int sample(int x, int z);
    }
}
