package com.letters.hitboxes;

import com.letters.hitboxes.config.ConfigManager;
import com.letters.hitboxes.gui.HitboxConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class HitboxKeybinds {

    private static final String CATEGORY = "key.categories.lettershitboxes";

    public static KeyMapping toggle;
    public static KeyMapping reload;
    public static KeyMapping toggleBoxes;
    public static KeyMapping openGui;
    public static KeyMapping nextProfile;
    public static KeyMapping toggleModels;

    private HitboxKeybinds() {
    }

    public static void register() {
        toggle = register("key.lettershitboxes.toggle", InputConstants.KEY_F6);
        openGui = register("key.lettershitboxes.gui", InputConstants.KEY_F7);
        nextProfile = register("key.lettershitboxes.profile", InputConstants.UNKNOWN.getValue());
        reload = register("key.lettershitboxes.reload", InputConstants.UNKNOWN.getValue());
        toggleBoxes = register("key.lettershitboxes.boxes", InputConstants.UNKNOWN.getValue());
        toggleModels = register("key.lettershitboxes.models", InputConstants.UNKNOWN.getValue());

        ClientTickEvents.END_CLIENT_TICK.register(HitboxKeybinds::tick);
    }

    private static KeyMapping register(String translationKey, int keyCode) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyMapping(translationKey, InputConstants.Type.KEYSYM, keyCode, CATEGORY));
    }

    private static void tick(Minecraft mc) {
        while (toggle.consumeClick()) {
            ConfigManager.get().enabled = !ConfigManager.get().enabled;
            ConfigManager.applyChanges();
            say(mc, "Letters Hitboxes: " + (ConfigManager.get().enabled ? "§aON" : "§cOFF"));
        }
        while (openGui.consumeClick()) {
            mc.setScreen(new HitboxConfigScreen(mc.screen));
        }
        while (nextProfile.consumeClick()) {
            String name = ConfigManager.nextProfile();
            say(mc, "§6Профиль: §f" + name);
        }
        while (reload.consumeClick()) {
            ConfigManager.load();
            say(mc, "§eLetters Hitboxes: конфиг перечитан");
        }
        while (toggleBoxes.consumeClick()) {
            ConfigManager.get().debug.renderBoxes = !ConfigManager.get().debug.renderBoxes;
            ConfigManager.applyChanges();
            say(mc, "Контуры хитбоксов: " + (ConfigManager.get().debug.renderBoxes ? "§aON" : "§cOFF"));
        }
        while (toggleModels.consumeClick()) {
            var visual = ConfigManager.profile().visual;
            visual.scaleModels = !visual.scaleModels;
            ConfigManager.applyChanges();
            say(mc, "Масштаб моделей: " + (visual.scaleModels ? "§aON" : "§cOFF"));
        }
    }

    private static void say(Minecraft mc, String message) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message), true);
        }
    }
}
