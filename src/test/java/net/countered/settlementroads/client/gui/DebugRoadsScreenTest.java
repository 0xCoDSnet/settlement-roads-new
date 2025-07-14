package net.countered.settlementroads.client.gui;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.List;

public class DebugRoadsScreenTest {
    @Test
    public void testDrawDoesNotThrow() {
        RoadGraph graph = new RoadGraph(
                List.of(new BlockPos(0,0,0), new BlockPos(5,0,5)),
                List.of(new RoadGraph.Edge(new BlockPos(0,0,0), new BlockPos(5,0,5)))
        );
        DebugRoadsScreen.SimpleDrawContext ctx = new DebugRoadsScreen.SimpleDrawContext() {
            public void drawHorizontalLine(int startX, int endX, int y, int color) {}
            public void drawVerticalLine(int startY, int endY, int x, int color) {}
        };
        DebugRoadsScreen.drawGraph(ctx, graph, 800, 600, BlockPos.ORIGIN, 1f, 0, 0);
    }
}
