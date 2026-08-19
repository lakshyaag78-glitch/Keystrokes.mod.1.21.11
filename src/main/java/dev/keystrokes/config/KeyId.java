package dev.keystrokes.config;

/**
 * The set of inputs the HUD can display. Order here also defines default
 * iteration order for rendering/config lists.
 */
public enum KeyId {
    W("W"),
    A("A"),
    S("S"),
    D("D"),
    SPACE("SPACE"),
    LMB("LMB"),
    RMB("RMB");

    private final String label;

    KeyId(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isMouseButton() {
        return this == LMB || this == RMB;
    }
}
