package dev.keystrokes;

import dev.keystrokes.hud.KeystrokesHud;
import dev.keystrokes.screen.KeystrokesConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only entrypoint (declared under "client" in fabric.mod.json).
 * Wires the HUD renderer into the vanilla HUD layer stack and registers a
 * keybinding to open the config screen. Nothing here touches the server or
 * common code paths - this mod is 100% client-side.
 */
public final class KeystrokesClient implements ClientModInitializer {

    public static final String MOD_ID = "keystrokes";
    
private static final Category KEY_CATEGORY = Category.register(Identifier.of(MOD_ID, "keystrokes"));
    private static KeyBinding openConfigKey;

    @Override
    public void onInitializeClient() {
        KeystrokesHud hud = new KeystrokesHud();

        HudElementRegistry.addLast(Identifier.of(MOD_ID, "keystrokes_hud"), hud::render);

        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.keystrokes.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // unbound by default; player assigns one in Controls
                KEY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Input/CPS/animation-target state updates live here, independent of
            // whether the HUD element is actually drawn this frame - see
            // KeystrokesHud.tick() for why.
            hud.tick(client);

            while (openConfigKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new KeystrokesConfigScreen(null));
                }
            }
        });
    }

    /** Convenience accessor used by the ModMenu integration to open the same screen. */
    public static KeystrokesConfigScreen createConfigScreen(net.minecraft.client.gui.screen.Screen parent) {
        return new KeystrokesConfigScreen(parent);
    }
}
