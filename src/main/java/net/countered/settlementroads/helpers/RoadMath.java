package net.countered.settlementroads.helpers;

import net.countered.settlementroads.features.RoadFeature;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;

import java.util.*;

public class RoadMath {
    /*
    public static List<BlockPos> calculateStraightPath(BlockPos start, BlockPos end, int width) {
        List<BlockPos> path = new ArrayList<>();
        int deltaX = end.getX() - start.getX();
        int deltaZ = end.getZ() - start.getZ();
        double steps = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        for (int i = 0; i <= steps; i++) {
            double t = i / steps;
            int x = (int) Math.round(start.getX() * (1 - t) + end.getX() * t);
            int z = (int) Math.round(start.getZ() * (1 - t) + end.getZ() * t);
            for (int w = (int) Math.round((double) -width / 2); w <= (int) Math.round((double) width / 2); w++) {
                path.add(new BlockPos(x + w, start.getY(), z));
            }
        }
        return path;
    }

     */

    // Dynamically calculate steps based on the road length
    public static int calculateDynamicSteps(BlockPos start, BlockPos end) {
        int deltaX = Math.abs(end.getX() - start.getX());
        int deltaZ = Math.abs(end.getZ() - start.getZ());

        // Calculate the straight-line distance
        return (int) Math.round(Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 4);
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

                for (double w = (double) -width / 2; w <= (double) width / 2; w += 0.5) {
                    int wx = (int) Math.round(xPos + px * w);
                    int wz = (int) Math.round(zPos + pz * w);
                    BlockPos centerPos = new BlockPos(wx, p1.getY(), wz);
                    // Add a small radius around the calculated position
                    double radius = 0.5;
                    for (double dxr = -radius; dxr <= radius; dxr += 0.5) {
                        for (double dzr = -radius; dzr <= radius; dzr += 0.5) {
                            double distance = Math.sqrt(dxr * dxr + dzr * dzr);
                            if (distance <= radius) {
                                int offsetX = (int) Math.round(centerPos.getX() + dxr);
                                int offsetZ = (int) Math.round(centerPos.getZ() + dzr);
                                BlockPos offsetPos = new BlockPos(offsetX, centerPos.getY(), offsetZ);
                                path.add(offsetPos);
                                RoadFeature.roadChunksCache.add(new ChunkPos(offsetPos));
                            }
                        }
                    }
                }
            }
        }
        return path;
    }


    public static List<BlockPos> generateControlPoints(BlockPos start, BlockPos end, Random random) {
        List<BlockPos> controlPoints = new ArrayList<>();
        controlPoints.add(start);

        double distance = start.getManhattanDistance(end);
        int count = (int) Math.round(distance / 300); // More points for longer roads
        int deviation = (int) Math.round(distance / 10); // Increase deviation with road length

        for (int i = 1; i < count; i++) {
            double t = i / (double) count;
            int x = (int) (start.getX() * (1 - t) + end.getX() * t);
            int z = (int) (start.getZ() * (1 - t) + end.getZ() * t);

            int deviationBound = Math.max(50, deviation / 2); // Ensure the bound is at least 50
            int orthogonalX = random.nextBetween(50, deviationBound) * (random.nextBoolean() ? 1 : -1);
            int orthogonalZ = random.nextBetween(50, deviationBound) * (random.nextBoolean() ? 1 : -1);

            // Ensure the new point is not moving backwards
            x = Math.max(Math.min(x + orthogonalX, Math.max(start.getX(), end.getX())), Math.min(start.getX(), end.getX()));
            z = Math.max(Math.min(z + orthogonalZ, Math.max(start.getZ(), end.getZ())), Math.min(start.getZ(), end.getZ()));

            controlPoints.add(new BlockPos(x, start.getY(), z));
        }

        controlPoints.add(end);
        return controlPoints;
    }

    // debugging
    public static void estimateMemoryUsage() {

        for (Map.Entry<Integer, Set<BlockPos>> entry : RoadFeature.roadBlocksCache.entrySet()) {
            System.out.println(entry.getValue().size());
        }
    }
}
