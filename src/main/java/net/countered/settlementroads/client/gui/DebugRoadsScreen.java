package net.countered.settlementroads.client.gui;

import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.persistence.attachments.WorldDataAttachment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class DebugRoadsScreen extends Screen {
    private static DebugRoadsScreen active;

    private static final HudRenderCallback HUD_CB = (ctx, tick) -> {
        if (active != null) active.onHudRender(ctx, tick);
    };

    static {
        HudRenderCallback.EVENT.register(HUD_CB);
    }

    private final MinecraftClient client;
    private float zoom = 2f;

    public DebugRoadsScreen(MinecraftClient client) {
        super(Text.literal("debug_roads"));
        this.client = client;
    }

    @Override
    protected void init() {
        active = this;
    }

    @Override
    public void close() {
        active = null;
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        zoom *= 1.0f + (float) verticalAmount * 0.1f;
        if (zoom < 0.5f) zoom = 0.5f;
        if (zoom > 10f) zoom = 10f;
        return true;
    }

    private void onHudRender(DrawContext drawContext, net.minecraft.client.render.RenderTickCounter tickCounter) {
        RoadGraph graph = buildGraph();
        drawGraph(new FabricDrawContext(drawContext), graph,
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight(),
                client.player.getBlockPos(), zoom,
                ModConfig.debugLineColor, ModConfig.debugNodeColor,
                drawContext, client);
    }

    private RoadGraph buildGraph() {
        List<BlockPos> nodes = new ArrayList<>();
        List<RoadGraph.Edge> edges = new ArrayList<>();
        if (client.world != null) {
            List<Records.RoadData> list = client.world.getAttached(WorldDataAttachment.ROAD_DATA_LIST);
            if (list != null) {
                for (Records.RoadData data : list) {
                    List<Records.RoadSegmentPlacement> segments = data.roadSegmentList();
                    for (int i = 0; i < segments.size(); i++) {
                        BlockPos pos = segments.get(i).middlePos();
                        nodes.add(pos);
                        if (i > 0) {
                            edges.add(new RoadGraph.Edge(segments.get(i - 1).middlePos(), pos));
                        }
                    }
                }
            }
        }
        return new RoadGraph(nodes, edges);
    }

    public static void drawGraph(SimpleDrawContext ctx, RoadGraph graph,
                                 int width, int height, BlockPos center, float zoom,
                                 int lineColor, int nodeColor) {
        drawGraph(ctx, graph, width, height, center, zoom, lineColor, nodeColor, null, null);
    }

    public static void drawGraph(SimpleDrawContext ctx, RoadGraph graph,
                                 int width, int height, BlockPos center, float zoom,
                                 int lineColor, int nodeColor,
                                 DrawContext dc, MinecraftClient client) {
        int originX = width / 2;
        int originY = height / 2;
        BlockPos hovered = null;
        double mouseX = client != null ? client.mouse.getX() * width / client.getWindow().getWidth() : 0;
        double mouseY = client != null ? client.mouse.getY() * height / client.getWindow().getHeight() : 0;

        for (RoadGraph.Edge edge : graph.edges()) {
            int x1 = originX + Math.round((edge.from().getX() - center.getX()) * zoom);
            int y1 = originY + Math.round((edge.from().getZ() - center.getZ()) * zoom);
            int x2 = originX + Math.round((edge.to().getX() - center.getX()) * zoom);
            int y2 = originY + Math.round((edge.to().getZ() - center.getZ()) * zoom);
            drawLine(ctx, x1, y1, x2, y2, lineColor);
        }

        for (BlockPos node : graph.nodes()) {
            int x = originX + Math.round((node.getX() - center.getX()) * zoom);
            int y = originY + Math.round((node.getZ() - center.getZ()) * zoom);
            ctx.drawHorizontalLine(x, x, y, nodeColor);
            ctx.drawVerticalLine(y, y, x, nodeColor);
            if (Math.abs(mouseX - x) < 4 && Math.abs(mouseY - y) < 4) {
                hovered = node;
            }
        }

        if (hovered != null && dc != null && client != null) {
            dc.drawText(client.textRenderer, hovered.toShortString(), (int) mouseX + 5,
                    (int) mouseY + 5, 0xFFFFFF, false);
        }
    }

    private static void drawLine(SimpleDrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        if (x1 == x2) {
            if (y1 > y2) {int t=y1; y1=y2; y2=t;}
            ctx.drawVerticalLine(y1, y2, x1, color);
        } else if (y1 == y2) {
            if (x1 > x2) {int t=x1; x1=x2; x2=t;}
            ctx.drawHorizontalLine(x1, x2, y1, color);
        } else {
            ctx.drawHorizontalLine(Math.min(x1,x2), Math.max(x1,x2), y1, color);
            ctx.drawVerticalLine(y1, y2, x2, color);
            ctx.drawVerticalLine(y1, y2, x1, color);
        }
    }

    public interface SimpleDrawContext {
        void drawHorizontalLine(int startX, int endX, int y, int color);
        void drawVerticalLine(int startY, int endY, int x, int color);
    }

    private record FabricDrawContext(DrawContext context) implements SimpleDrawContext {
        @Override
        public void drawHorizontalLine(int startX, int endX, int y, int color) {
            context.drawHorizontalLine(startX, endX, y, color);
        }

        @Override
        public void drawVerticalLine(int startY, int endY, int x, int color) {
            context.drawVerticalLine(x, startY, endY, color);
        }
    }
}
