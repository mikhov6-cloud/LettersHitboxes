package com.letters.hitboxes.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.letters.hitboxes.HitboxEngine;
import com.letters.hitboxes.LettersHitboxesClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(LettersHitboxesClient.MOD_ID + ".json");

    private static volatile HitboxConfig config = defaultConfig();

    private ConfigManager() {
    }

    public static HitboxConfig get() {
        return config;
    }

    /** Shortcut for the active profile. */
    public static HitboxProfile profile() {
        return config.active();
    }

    public static Path path() {
        return PATH;
    }

    private static HitboxConfig defaultConfig() {
        HitboxConfig cfg = new HitboxConfig();
        cfg.sanitize();
        return cfg;
    }

    public static synchronized void load() {
        HitboxConfig loaded = null;
        if (Files.exists(PATH)) {
            try {
                String json = Files.readString(PATH);
                loaded = GSON.fromJson(json, HitboxConfig.class);
            } catch (IOException | JsonSyntaxException e) {
                LettersHitboxesClient.LOGGER.error("[LettersHitboxes] Failed to read {}: {}", PATH, e.toString());
            }
        }
        if (loaded == null) {
            loaded = new HitboxConfig();
        }
        loaded.sanitize();
        config = loaded;
        save();
        HitboxEngine.invalidate();
        LettersHitboxesClient.LOGGER.info("[LettersHitboxes] Config loaded (enabled={}, profile='{}', profiles={})",
                config.enabled, config.activeProfile, config.profiles.keySet());
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(config));
        } catch (IOException e) {
            LettersHitboxesClient.LOGGER.error("[LettersHitboxes] Failed to write {}: {}", PATH, e.toString());
        }
    }

    /** Overwrites the config with the built-in defaults. */
    public static synchronized void reset() {
        config = defaultConfig();
        applyChanges();
    }

    /** Save + drop caches + refresh every loaded entity. Call after any runtime change. */
    public static void applyChanges() {
        config.sanitize();
        save();
        HitboxEngine.invalidate();
    }

    // ------------------------------------------------------------------ profiles

    public static List<String> profileNames() {
        return new ArrayList<>(config.profiles.keySet());
    }

    public static boolean useProfile(String name) {
        if (!config.profiles.containsKey(name)) return false;
        config.activeProfile = name;
        applyChanges();
        return true;
    }

    /** Switches to the next profile in file order and returns its name. */
    public static String nextProfile() {
        List<String> names = profileNames();
        if (names.isEmpty()) return config.activeProfile;
        int index = names.indexOf(config.activeProfile);
        String next = names.get((index + 1) % names.size());
        useProfile(next);
        return next;
    }

    /** Creates a profile as a copy of the active one (or of {@code from} when given). */
    public static boolean createProfile(String name, String from) {
        if (name == null || name.isBlank() || config.profiles.containsKey(name)) return false;
        HitboxProfile source = from != null ? config.profiles.get(from) : config.active();
        if (source == null) return false;
        HitboxProfile copy = source.copy();
        copy.description = "Copy of " + (from != null ? from : config.activeProfile);
        config.profiles.put(name, copy);
        applyChanges();
        return true;
    }

    public static boolean deleteProfile(String name) {
        if (config.profiles.size() <= 1 || !config.profiles.containsKey(name)) return false;
        config.profiles.remove(name);
        if (name.equals(config.activeProfile)) {
            config.activeProfile = config.profiles.keySet().iterator().next();
        }
        applyChanges();
        return true;
    }

    /** Restores the four built-in presets without touching custom profiles. */
    public static void restorePresets() {
        HitboxProfile.presets().forEach((name, preset) -> config.profiles.put(name, preset));
        applyChanges();
    }
}
