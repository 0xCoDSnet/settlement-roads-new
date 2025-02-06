package net.countered.settlementroads.helpers;

import net.countered.settlementroads.features.RoadFeature;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;

import java.util.*;

public class RoadMath {
    // Dynamically calculate steps based on the road length
    public static int calculateDynamicSteps(BlockPos start, BlockPos end) {
        int deltaX = Math.abs(end.getX() - start.getX());
        int deltaZ = Math.abs(end.getZ() - start.getZ());

        // Calculate the straight-line distance
        return (int) Math.round(Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 5);
    }

    public static Map<BlockPos, Records.RoadSegmentData> calculateSplinePath(List<BlockPos> controlPoints, int width, int steps) {
        Map<BlockPos, Records.RoadSegmentData> roadSegments = new LinkedHashMap<>();
        Set<BlockPos> middlePositions = new LinkedHashSet<>();  // Track middle blocks globally

        for (int i = 0; i < controlPoints.size() - 1; i++) {
            BlockPos p0 = controlPoints.get(Math.max(0, i - 1));
            BlockPos p1 = controlPoints.get(i);
            BlockPos p2 = controlPoints.get(i + 1);
            BlockPos p3 = controlPoints.get(Math.min(controlPoints.size() - 1, i + 2));

            for (int j = 0; j < steps; j++) {
                double t = j / (double) steps;
                double x = 0.5 * ((2 * p1.getX()) +
                        (-p0.getX() + p2.getX()) * t +
                        (2 * p0.getX() - 5 * p1.getX() + 4 * p2.getX() - p3.getX()) * t * t +
                        (-p0.getX() + 3 * p1.getX() - 3 * p2.getX() + p3.getX()) * t * t * t);
                double z = 0.5 * ((2 * p1.getZ()) +
                        (-p0.getZ() + p2.getZ()) * t +
                        (2 * p0.getZ() - 5 * p1.getZ() + 4 * p2.getZ() - p3.getZ()) * t * t +
                        (-p0.getZ() + 3 * p1.getZ() - 3 * p2.getZ() + p3.getZ()) * t * t * t);

                int xPos = (int) Math.round(x);
                int zPos = (int) Math.round(z);
                BlockPos centerPos = new BlockPos(xPos, 0, zPos);
                RoadFeature.roadChunksCache.add(new ChunkPos(centerPos));
                // Generate road segment
                Records.RoadSegmentData segment = generateRoadWidth(centerPos, p0, p2, width, middlePositions);

                // Store middle block position
                middlePositions.add(segment.middle());

                // Store segment in map (ensuring middle blocks take priority)
                roadSegments.put(segment.middle(), segment);
            }
        }
        return roadSegments;
    }

    private static Records.RoadSegmentData generateRoadWidth(BlockPos center, BlockPos prev, BlockPos next, int width, Set<BlockPos> middlePositions) {
        Set<BlockPos> widthPositions = new LinkedHashSet<>();

        // Calculate tangent vector (direction of the road)
        double dx = next.getX() - prev.getX();
        double dz = next.getZ() - prev.getZ();

        // Normalize the tangent to get the perpendicular vector
        double length = Math.sqrt(dx * dx + dz * dz);
        double px = -dz / length;  // Perpendicular x
        double pz = dx / length;   // Perpendicular z

        // Add middle block
        BlockPos middle = new BlockPos(center.getX(), 0, center.getZ());

        // Create road width using perpendicular vector
        for (double w = -width / 2.0; w <= width / 2.0d; w += 0.1d) {


            int fx = (int) Math.round(center.getX() + px * w);
            int fz = (int) Math.round(center.getZ() + pz * w);
            BlockPos sideBlockPos = new BlockPos(fx, 0, fz);
            // Only add width block if it's NOT already a middle block
            if (!middlePositions.contains(sideBlockPos)) {
                widthPositions.add(sideBlockPos);
                RoadFeature.roadChunksCache.add(new ChunkPos(sideBlockPos));
            }
        }
        return new Records.RoadSegmentData(middle, widthPositions);
    }

    public static List<BlockPos> generateControlPoints(BlockPos start, BlockPos end, Random random) {
        List<BlockPos> controlPoints = new ArrayList<>();

        // Calculate vectors from start to end
        double dx = end.getX() - start.getX();
        double dz = end.getZ() - start.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        // Normalize the vector and scale it to 50 blocks
        double scale = 50 / distance;
        double offsetX = dx * scale;
        double offsetZ = dz * scale;

        // Offset the start and end points by 50 blocks
        BlockPos adjustedStart = new BlockPos((int) (start.getX() + offsetX), start.getY(), (int) (start.getZ() + offsetZ));
        BlockPos adjustedEnd = new BlockPos((int) (end.getX() - offsetX), end.getY(), (int) (end.getZ() - offsetZ));

        controlPoints.add(adjustedStart);

        double totalDistance = adjustedStart.getManhattanDistance(adjustedEnd);
        int step = 150;
        int deviationBound = 40;
        int count = (int) Math.ceil(totalDistance / step);

        for (int i = 1; i < count; i++) {
            double t = i / (double) count;
            int x = (int) (adjustedStart.getX() * (1 - t) + adjustedEnd.getX() * t);
            int z = (int) (adjustedStart.getZ() * (1 - t) + adjustedEnd.getZ() * t);

            int orthogonalX = random.nextBetween(20, deviationBound) * (random.nextBoolean() ? 1 : -1);
            int orthogonalZ = random.nextBetween(20, deviationBound) * (random.nextBoolean() ? 1 : -1);

            x += orthogonalX;
            z += orthogonalZ;

            controlPoints.add(new BlockPos(x, 0, z));
        }

        controlPoints.add(adjustedEnd);
        return controlPoints;
    }


    // debugging
    public static void estimateMemoryUsage() {
        /*
        for (Map.Entry<Integer, Set<BlockPos>> entry : RoadFeature.roadBlocksCache.entrySet()) {
            System.out.println(entry.getValue().size());
        }
        */
        System.out.println(RoadFeature.roadChunksCache.size());
        System.out.println(RoadFeature.roadBlocksCache.size());
        System.out.println(RoadFeature.roadSegmentsCache.size());
    }
}
