package net.countered.settlementroads.client.gui;

import net.minecraft.util.math.BlockPos;
import java.util.List;

public record RoadGraph(List<BlockPos> nodes, List<Edge> edges) {
    public record Edge(BlockPos from, BlockPos to) {}
}
