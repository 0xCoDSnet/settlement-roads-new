package net.countered.settlementroads.features.roadlogic.pathfinding;

import net.countered.settlementroads.helpers.Records;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public class RoadPathCalculator {
    public static List<Records.RoadSegmentPlacement> calculateAStarRoadPath(
            BlockPos start, BlockPos end, int width, HeightmapSampler heightSampler
    ) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();

        BlockPos startGround = new BlockPos(start.getX(), heightSampler.sample(start.getX(), start.getZ()), start.getZ());
        BlockPos endGround = new BlockPos(end.getX(), heightSampler.sample(end.getX(), end.getZ()), end.getZ());

        Node startNode = new Node(startGround, null, 0.0, heuristic(startGround, endGround));
        openSet.add(startNode);
        allNodes.put(startGround, startNode);

        int maxSteps = 10000;

        while (!openSet.isEmpty() && maxSteps-- > 0) {
            System.out.println(maxSteps);
            Node current = openSet.poll();

            if (current.pos.getX() == endGround.getX() && current.pos.getZ() == endGround.getZ()) {
                return reconstructPath(current, width, heightSampler);
            }

            closedSet.add(current.pos);

            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos neighborPos = current.pos.offset(dir);
                int y = heightSampler.sample(neighborPos.getX(), neighborPos.getZ());
                neighborPos = new BlockPos(neighborPos.getX(), y, neighborPos.getZ());

                if (closedSet.contains(neighborPos)) continue;

                double elevationCost = Math.abs(y - current.pos.getY()); // Avoid steep elevation differences
                if (elevationCost > 3) {
                    System.out.println("skipping");
                    continue; // Skip if elevation difference is too high
                }

                double tentativeG = current.gScore + 1 + elevationCost;

                Node neighbor = allNodes.get(neighborPos);
                if (neighbor == null || tentativeG < neighbor.gScore) {
                    double h = heuristic(neighborPos, endGround);
                    neighbor = new Node(neighborPos, current, tentativeG, tentativeG + h);
                    allNodes.put(neighborPos, neighbor);
                    openSet.add(neighbor);
                }
            }
        }
        System.out.println("no suitable path found");
        return Collections.emptyList(); // No path found
    }

    private static List<Records.RoadSegmentPlacement> reconstructPath(Node endNode, int width, HeightmapSampler sampler) {
        Map<BlockPos, Set<BlockPos>> roadSegments = new LinkedHashMap<>();
        Set<BlockPos> widthCache = new HashSet<>();
        Node current = endNode;

        while (current != null) {
            BlockPos pos = current.pos;
            BlockPos prev = current.parent != null ? current.parent.pos : pos;
            Set<BlockPos> widthSet = generateWidth(pos, prev, pos, width, widthCache);
            roadSegments.put(pos, widthSet);
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
        return dxzDistance * 2 + elevationDifference;
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

    private static Set<BlockPos> generateWidth(BlockPos center, BlockPos prev, BlockPos next, int width, Set<BlockPos> widthPositionsCache) {
        Set<BlockPos> segmentWidthPositions = new HashSet<>();
        double adjustedWidth = width - 0.05d;
        // Calculate tangent vector (direction of the road)
        int dx = next.getX() - prev.getX();
        int dz = next.getZ() - prev.getZ();
        // Normalize the tangent to get the perpendicular vector
        double length = Math.sqrt(dx * dx + dz * dz);

        double tangentX = dx / length;  // Tangent x
        double tangentZ = dz / length;  // Tangent z
        // Perpendicular vector (normal to the tangent)
        double px = -tangentZ;  // Perpendicular x
        double pz = tangentX;   // Perpendicular z

        // Create road width using perpendicular vector
        List<BlockPos> widthLine = getStraightLine(
                new BlockPos(center.getX() - (int) Math.round(px * (adjustedWidth / 2d)), center.getY(), center.getZ() - (int) Math.round(pz * (adjustedWidth / 2d))),
                new BlockPos(center.getX() + (int) Math.round(px * (adjustedWidth / 2d)), center.getY(), center.getZ() + (int) Math.round(pz * (adjustedWidth / 2d)))
        );
        for (BlockPos widthBlockPos : widthLine) {
            if (widthPositionsCache.contains(widthBlockPos)) {
                continue;
            }
            widthPositionsCache.add(widthBlockPos);
            segmentWidthPositions.add(widthBlockPos);
        }
        return segmentWidthPositions;
    }

    public static List<BlockPos> getStraightLine(BlockPos start, BlockPos end) {
        List<BlockPos> lineBlocks = new ArrayList<>();

        int x1 = start.getX(), z1 = start.getZ();
        int x2 = end.getX(), z2 = end.getZ();

        int dx = Math.abs(x2 - x1);
        int dz = Math.abs(z2 - z1);
        int sx = x1 < x2 ? 1 : -1;
        int sz = z1 < z2 ? 1 : -1;
        int err = dx - dz;

        while (true) {
            lineBlocks.add(new BlockPos(x1, 0, z1));

            if (x1 == x2 && z1 == z2) break;
            int e2 = err * 2;

            if (e2 > -dz) {
                err -= dz;
                x1 += sx;
            } else if (e2 < dx) {
                err += dx;
                z1 += sz;
            }
        }
        return lineBlocks;
    }

    public interface HeightmapSampler {
        int sample(int x, int z);
    }
}
