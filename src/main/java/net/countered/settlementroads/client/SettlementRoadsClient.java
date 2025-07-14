package net.countered.settlementroads.client;

import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.client.gui.DebugRoadsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SettlementRoadsClient implements ClientModInitializer {
    private static KeyBinding debugKey;

    @Override
    public void onInitializeClient() {
        debugKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.settlement-roads.debug_map",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.settlement-roads"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (debugKey.wasPressed()) {
                if (ModConfig.enableDebugMap && client.currentScreen == null) {
                    client.setScreen(new DebugRoadsScreen(client));
                }
            }
        });
    }
}
