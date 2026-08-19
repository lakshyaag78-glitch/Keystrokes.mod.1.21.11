package dev.keystrokes.input;

import dev.keystrokes.config.KeyId;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiny event bus that {@code dev.keystrokes.mixin.KeyBindingMixin} publishes
 * raw keybinding press/release edges to, translated here into our own
 * {@link KeyId} click events for LMB/RMB only - the only keys that need
 * discrete click detection. W/A/S/D/SPACE only ever need the continuous
 * held-state polling in {@link InputTracker}; conflating the two would blur
 * "is this held" and "did this just get clicked" into one signal, which is
 * exactly the mixing this class exists to avoid (see {@code CpsTracker} vs.
 * {@code Animation} for how each signal is actually consumed).
 * <p>
 * All the domain logic (which binding is "LMB", focus/world gating, etc.)
 * lives here in ordinary code - the mixin itself is a one-line, zero-logic
 * observer. This class never cancels, modifies, or consumes anything; it
 * only reads {@link KeyBinding#isPressed()}-equivalent edge notifications
 * that Minecraft was already going to deliver to the binding regardless of
 * whether this mod exists.
 */
public final class MouseClickEvents {

    @FunctionalInterface
    public interface Listener {
        /** Fired on the press-down edge only (never on release), already focus/world-gated. */
        void onClick(KeyId id);
    }

    private static final List<Listener> LISTENERS = new ArrayList<>();

    private MouseClickEvents() {
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    /**
     * Called by the mixin for every keybinding's press/release edge - every
     * keybinding in the game, not just ours (movement, inventory, hotbar
     * slots, etc.). Cheap: a couple of reference comparisons, no allocation,
     * and it does nothing at all unless the edge is a press-down that
     * matches the current attack or use binding.
     */
    public static void onKeyBindingEdge(KeyBinding binding, boolean pressed) {
        if (!pressed) {
            return; // Releases are not clicks; CPS only counts press-down edges.
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options == null || !client.isWindowFocused()) {
            return;
        }

        KeyId id;
        if (binding == client.options.attackKey) {
            id = KeyId.LMB;
        } else if (binding == client.options.useKey) {
            id = KeyId.RMB;
        } else {
            return; // Not a binding we track clicks for.
        }

        for (Listener listener : LISTENERS) {
            listener.onClick(id);
        }
    }
}
