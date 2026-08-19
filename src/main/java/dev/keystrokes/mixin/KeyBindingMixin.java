package dev.keystrokes.mixin;

import dev.keystrokes.input.MouseClickEvents;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mod's only mixin, and it is a pure, side-effect-free observer.
 * <p>
 * Fabric API does not expose a public event for "a keybinding's raw
 * press/release edge, independent of tick polling", and polling
 * {@code KeyBinding.isPressed()} at client-tick rate (~20 Hz) can miss a
 * genuine press+release that both happen between two ticks - unacceptable
 * for a CPS counter. {@link KeyBinding#setPressed(boolean)} is called
 * directly from Minecraft's own GLFW input handling every time a bound key's
 * physical state actually changes, so injecting here observes clicks at full
 * input resolution instead of sampling.
 * <p>
 * The injection is a single {@code @Inject} at the very head of the method,
 * with no {@code cancellable} flag, no local capture, and no modification of
 * any field or return value - it only reports what already happened to
 * {@link MouseClickEvents} and returns immediately. Vanilla's own
 * press/release handling (including the actual attack/use gameplay actions
 * that read this same binding elsewhere) is completely unaffected; this
 * class cannot consume, cancel, or alter input in any way.
 */
@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {

    @Inject(method = "setPressed", at = @At("HEAD"))
    private void keystrokes$onSetPressed(boolean pressed, CallbackInfo ci) {
        MouseClickEvents.onKeyBindingEdge((KeyBinding) (Object) this, pressed);
    }
}
