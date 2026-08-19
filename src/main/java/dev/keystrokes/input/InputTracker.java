package dev.keystrokes.input;

import dev.keystrokes.config.KeyId;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

/**
 * Pure observer of Minecraft's own input state.
 * <p>
 * This never calls anything that consumes, blocks, or rebinds an input event -
 * it only reads {@link KeyBinding#isPressed()}, which is the same state
 * vanilla uses to move the player / swing / use items. That means the mod can
 * never desync from or interfere with actual gameplay input, and correctly
 * follows the player's real keybinding configuration (not hardcoded GLFW
 * scancodes) as required for W/A/S/D/Space/Attack/Use.
 */
public final class InputTracker {

    private final MinecraftClient client = MinecraftClient.getInstance();

    /**
     * @return whether the given tracked key is currently held, or {@code false}
     * if the game window doesn't have focus (prevents "stuck pressed" visuals
     * and CPS inflation when alt-tabbed).
     */
    public boolean isPressed(KeyId id) {
        if (client == null || client.getWindow() == null || !client.isWindowFocused()) {
            return false;
        }
        KeyBinding binding = bindingFor(id);
        return binding != null && binding.isPressed();
    }

    private KeyBinding bindingFor(KeyId id) {
        if (client.options == null) {
            return null;
        }
        return switch (id) {
            case W -> client.options.forwardKey;
            case A -> client.options.leftKey;
            case S -> client.options.backKey;
            case D -> client.options.rightKey;
            case SPACE -> client.options.jumpKey;
            case LMB -> client.options.attackKey;
            case RMB -> client.options.useKey;
        };
    }
}
