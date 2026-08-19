package dev.keystrokes.config;

import dev.keystrokes.hud.AnimationProfile;

/**
 * Built-in presets. Each one only touches visual/animation settings - position,
 * per-key enable state and CPS visibility are left as the player configured them,
 * since a preset is a "look", not a full reset. Every preset here changes more
 * than just a color or two: shape, spacing, motion character and palette all
 * shift together so each one reads as a genuinely distinct style.
 */
public enum Preset {

    CLEAN("Clean") {
        @Override
        public void apply(KeystrokesConfig c) {
            c.keyWidth = 24f;
            c.keyHeight = 22f;
            c.spacingH = 4f;
            c.spacingV = 4f;
            c.cornerRadius = 6;
            c.backgroundColor = 0xFFFFFF;
            c.backgroundOpacity = 0.10f;
            c.backgroundPressedColor = 0xFFFFFF;
            c.backgroundPressedOpacity = 0.30f;
            c.borderEnabled = true;
            c.borderColor = 0xFFFFFF;
            c.borderOpacity = 0.35f;
            c.borderThickness = 1f;
            c.textColor = 0xFFFFFF;
            c.textPressedColor = 0xFFFFFF;
            c.textSize = 1.0f;
            c.animationProfile = AnimationProfile.SOFT;
            c.scaleAnimationEnabled = true;
            c.moveAnimationEnabled = true;
            c.colorAnimationEnabled = true;
            c.pressScaleAmount = 0.04f;
            c.pressMoveAmount = 1.5f;
        }
    },
    MINIMAL("Minimal") {
        @Override
        public void apply(KeystrokesConfig c) {
            c.keyWidth = 20f;
            c.keyHeight = 18f;
            c.spacingH = 2f;
            c.spacingV = 2f;
            c.cornerRadius = 2;
            c.backgroundColor = 0x000000;
            c.backgroundOpacity = 0.35f;
            c.backgroundPressedColor = 0x555555;
            c.backgroundPressedOpacity = 0.55f;
            c.borderEnabled = false;
            c.textColor = 0xCFCFCF;
            c.textPressedColor = 0xFFFFFF;
            c.textSize = 0.9f;
            c.animationProfile = AnimationProfile.SMOOTH;
            c.scaleAnimationEnabled = true;
            c.moveAnimationEnabled = true;
            c.colorAnimationEnabled = true;
            c.pressScaleAmount = 0.03f;
            c.pressMoveAmount = 1.0f;
        }
    },
    CLASSIC("Classic") {
        @Override
        public void apply(KeystrokesConfig c) {
            c.keyWidth = 20f;
            c.keyHeight = 20f;
            c.spacingH = 2f;
            c.spacingV = 2f;
            c.cornerRadius = 0;
            c.backgroundColor = 0x8B8B8B;
            c.backgroundOpacity = 0.65f;
            c.backgroundPressedColor = 0xFFFFFF;
            c.backgroundPressedOpacity = 0.85f;
            c.borderEnabled = true;
            c.borderColor = 0x000000;
            c.borderOpacity = 0.8f;
            c.borderThickness = 1f;
            c.textColor = 0x000000;
            c.textPressedColor = 0x000000;
            c.animationProfile = AnimationProfile.INSTANT;
            c.scaleAnimationEnabled = false;
            c.moveAnimationEnabled = false;
            c.colorAnimationEnabled = false;
            c.pressScaleAmount = 0f;
            c.pressMoveAmount = 0f;
        }
    },
    PVP("PvP") {
        @Override
        public void apply(KeystrokesConfig c) {
            c.keyWidth = 20f;
            c.keyHeight = 18f;
            c.spacingH = 2f;
            c.spacingV = 2f;
            c.cornerRadius = 3;
            c.backgroundColor = 0x0A0A0F;
            c.backgroundOpacity = 0.65f;
            c.backgroundPressedColor = 0xE23A3A;
            c.backgroundPressedOpacity = 0.95f;
            c.borderEnabled = true;
            c.borderColor = 0xFFFFFF;
            c.borderOpacity = 0.25f;
            c.borderThickness = 1f;
            c.textColor = 0xE6E6EC;
            c.textPressedColor = 0xFFFFFF;
            c.animationProfile = AnimationProfile.SNAPPY;
            c.scaleAnimationEnabled = true;
            c.moveAnimationEnabled = true;
            c.colorAnimationEnabled = true;
            c.pressScaleAmount = 0.07f;
            c.pressMoveAmount = 2.5f;
        }
    },
    GLASS("Glass") {
        @Override
        public void apply(KeystrokesConfig c) {
            c.keyWidth = 24f;
            c.keyHeight = 22f;
            c.spacingH = 5f;
            c.spacingV = 5f;
            c.cornerRadius = 9;
            c.backgroundColor = 0xBFD9FF;
            c.backgroundOpacity = 0.12f;
            c.backgroundPressedColor = 0xBFD9FF;
            c.backgroundPressedOpacity = 0.32f;
            c.borderEnabled = true;
            c.borderColor = 0xFFFFFF;
            c.borderOpacity = 0.45f;
            c.borderThickness = 1f;
            c.textColor = 0xF5F8FF;
            c.textPressedColor = 0xFFFFFF;
            c.textSize = 1.0f;
            c.animationProfile = AnimationProfile.SOFT;
            c.scaleAnimationEnabled = true;
            c.moveAnimationEnabled = true;
            c.colorAnimationEnabled = true;
            c.pressScaleAmount = 0.05f;
            c.pressMoveAmount = 1.5f;
        }
    },
    DARK("Dark") {
        @Override
        public void apply(KeystrokesConfig c) {
            c.keyWidth = 22f;
            c.keyHeight = 20f;
            c.spacingH = 3f;
            c.spacingV = 3f;
            c.cornerRadius = 3;
            c.backgroundColor = 0x000000;
            c.backgroundOpacity = 0.70f;
            c.backgroundPressedColor = 0x2A2A32;
            c.backgroundPressedOpacity = 0.95f;
            c.borderEnabled = true;
            c.borderColor = 0x2E2E36;
            c.borderOpacity = 0.9f;
            c.borderThickness = 1f;
            c.textColor = 0xE8E8EC;
            c.textPressedColor = 0xFFFFFF;
            c.textSize = 1.0f;
            c.animationProfile = AnimationProfile.SMOOTH;
            c.scaleAnimationEnabled = true;
            c.moveAnimationEnabled = true;
            c.colorAnimationEnabled = true;
            c.pressScaleAmount = 0.04f;
            c.pressMoveAmount = 1.5f;
        }
    },
    ACCENT("Accent") {
        @Override
        public void apply(KeystrokesConfig c) {
            c.keyWidth = 22f;
            c.keyHeight = 20f;
            c.spacingH = 3f;
            c.spacingV = 3f;
            c.cornerRadius = 5;
            c.backgroundColor = 0x0F0B1A;
            c.backgroundOpacity = 0.60f;
            c.backgroundPressedColor = 0x9B5CF6;
            c.backgroundPressedOpacity = 0.9f;
            c.borderEnabled = true;
            c.borderColor = 0x9B5CF6;
            c.borderOpacity = 0.5f;
            c.borderThickness = 1f;
            c.textColor = 0xE8E0FF;
            c.textPressedColor = 0xFFFFFF;
            c.textSize = 1.0f;
            c.animationProfile = AnimationProfile.SNAPPY;
            c.scaleAnimationEnabled = true;
            c.moveAnimationEnabled = true;
            c.colorAnimationEnabled = true;
            c.pressScaleAmount = 0.06f;
            c.pressMoveAmount = 2.0f;
        }
    };

    private final String displayName;

    Preset(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public abstract void apply(KeystrokesConfig config);

    public static Preset byName(String name) {
        for (Preset p : values()) {
            if (p.displayName.equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }
}
