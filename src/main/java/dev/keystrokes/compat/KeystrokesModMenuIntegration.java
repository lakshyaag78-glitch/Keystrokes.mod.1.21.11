package dev.keystrokes.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.keystrokes.KeystrokesClient;

/**
 * Registered via the "modmenu" entrypoint in fabric.mod.json. This class is
 * only ever loaded by Mod Menu itself, when Mod Menu is present - Fabric
 * Loader does not eagerly instantiate entrypoints that nothing asks for, so
 * this mod works completely normally with Mod Menu absent (it's a
 * modCompileOnly dependency, not bundled and not required at runtime).
 */
public final class KeystrokesModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return KeystrokesClient::createConfigScreen;
    }
}
