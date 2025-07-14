package net.countered.settlementroads.client;

import net.countered.settlementroads.client.debug.*;
import org.junit.jupiter.api.Test;
import java.util.List;

public class DebugRoadsScreenTest {
    static class TestClient implements GameClient {
        @Override public int getScaledWidth() { return 200; }
        @Override public int getScaledHeight() { return 150; }
        @Override public double getMouseX() { return 0; }
        @Override public double getMouseY() { return 0; }
    }

    static class DummyDraw implements DrawAdapter {
        @Override public void drawHorizontalLine(int x1, int x2, int y, int color) {}
        @Override public void drawVerticalLine(int x, int y1, int y2, int color) {}
        @Override public void drawText(String text, int x, int y, int color) {}
    }

    @Test
    public void drawNoNPE() {
        RoadGraph graph = new RoadGraph() {
            @Override public java.util.Collection<Node> nodes() { return List.of(new Node(0,0,"")); }
            @Override public java.util.Collection<Edge> edges() { return List.of(new Edge(new Node(0,0,""), new Node(1,1,""))); }
        };
        DebugRoadsScreen screen = new DebugRoadsScreen(new TestClient(), graph);
        screen.drawGraph(new DummyDraw());
    }
}
