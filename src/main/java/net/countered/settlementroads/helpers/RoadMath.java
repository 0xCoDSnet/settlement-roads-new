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
        return (int) Math.round(Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 2);
    }

    public static Set<BlockPos> calculateSplinePath(List<BlockPos> controlPoints, int width, int steps) {
        Set<BlockPos> path = new HashSet<BlockPos>();

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
                        (-p0.getX() + 3 * p1.getX() - 3 * p2.getX() + p3.getX()) * t * t * t
                );
                double z = 0.5 * ((2 * p1.getZ()) +
                        (-p0.getZ() + p2.getZ()) * t +
                        (2 * p0.getZ() - 5 * p1.getZ() + 4 * p2.getZ() - p3.getZ()) * t * t +
                        (-p0.getZ() + 3 * p1.getZ() - 3 * p2.getZ() + p3.getZ()) * t * t * t
                );

                int xPos = (int) Math.round(x);
                int zPos = (int) Math.round(z);

                // Calculate tangent vector at the current point
                double dx = 0.5 * ((-p0.getX() + p2.getX()) +
                        2 * (2 * p0.getX() - 5 * p1.getX() + 4 * p2.getX() - p3.getX()) * t +
                        3 * (-p0.getX() + 3 * p1.getX() - 3 * p2.getX() + p3.getX()) * t * t
                );
                double dz = 0.5 * ((-p0.getZ() + p2.getZ()) +
                        2 * (2 * p0.getZ() - 5 * p1.getZ() + 4 * p2.getZ() - p3.getZ()) * t +
                        3 * (-p0.getZ() + 3 * p1.getZ() - 3 * p2.getZ() + p3.getZ()) * t * t
                );

                // Normalize the tangent vector to get the perpendicular vector
                double length = Math.sqrt(dx * dx + dz * dz);
                double px = -dz / length;
                double pz = dx / length;

                for (double w =  (-width / 2.0); w <= (width / 2.0); w += 0.1) {
                    int wx = (int) Math.round(xPos + px * w);
                    int wz = (int) Math.round(zPos + pz * w);
                    BlockPos placePos = new BlockPos(wx, 0, wz);
                    path.add(placePos);
                    RoadFeature.roadChunksCache.add(new ChunkPos(placePos));
                }
            }
        }
        return path;
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

        for (Map.Entry<Integer, Set<BlockPos>> entry : RoadFeature.roadBlocksCache.entrySet()) {
            System.out.println(entry.getValue().size());
        }
    }
}
