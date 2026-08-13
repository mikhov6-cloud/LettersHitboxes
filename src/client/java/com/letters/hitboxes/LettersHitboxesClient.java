package com.letters.hitboxes;

import com.letters.hitboxes.command.HitboxCommand;
import com.letters.hitboxes.config.ConfigManager;
import com.letters.hitboxes.render.HitboxOutlineRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-only entry point. Nothing in this mod runs on, or talks to, a server. */
public class LettersHitboxesClient implements ClientModInitializer {

    public static final String MOD_ID = "lettershitboxes";
    public static final Logger LOGGER = LoggerFactory.getLogger("LettersHitboxes");

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        HitboxKeybinds.register();

        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> HitboxCommand.register(dispatcher));

        WorldRenderEvents.AFTER_ENTITIES.register(HitboxOutlineRenderer::render);

        // caches depend on "am I in singleplayer?" and on the local player id
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> HitboxEngine.invalidate());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> HitboxEngine.invalidate());

        LOGGER.info("[LettersHitboxes] client initialised - config: {}", ConfigManager.path());
    }
}
