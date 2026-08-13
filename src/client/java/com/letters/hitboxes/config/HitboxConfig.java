package com.letters.hitboxes.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Root of {@code config/lettershitboxes.json}. */
public class HitboxConfig {

    public String __help = "Letters Hitboxes (client side). Rules live inside profiles - switch with "
            + "/hitbox profile use <name> or the GUI (F7). width/height are multipliers (1.0 = vanilla). "
            + "mode: OFF | DIMENSIONS (real physical hitbox) | TARGETING (ray-cast box only) | BOTH. "
            + "Priority inside a profile: blacklist > byEntityId > byCategory > selfPlayer/otherPlayers > defaults. "
            + "Any omitted field is inherited from the parent rule.";

    /** Master switch. */
    public boolean enabled = true;

    /** Apply changes while connected to a remote (multiplayer) server. The server always keeps its own
     *  vanilla hitboxes, so this only changes YOUR client: expect desync. Not a way to hit further -
     *  the server validates every attack. Singleplayer / LAN host ignores this flag. */
    public boolean applyOnMultiplayer = false;

    /** Hard safety clamp for every multiplier. */
    public float maxScale = 8.0F;

    /** Never enlarge the local player vertically (avoids suffocating in 2-block corridors). */
    public boolean protectSelfFromSuffocation = true;

    /** Recalculate all loaded entity hitboxes automatically after any config change. */
    public boolean autoRefreshEntities = true;

    /** Name of the profile currently in use. */
    public String activeProfile = "default";

    /** All available profiles. */
    public Map<String, HitboxProfile> profiles = HitboxProfile.presets();

    /** Debug rendering. */
    public Debug debug = new Debug();

    // ---------------------------------------------------------------- legacy (pre-profile) fields
    // kept only so an old config file keeps working; migrated into a profile on load, then dropped.
    public ScaleRule defaults;
    public ScaleRule selfPlayer;
    public ScaleRule otherPlayers;
    public Map<String, ScaleRule> byCategory;
    public Map<String, ScaleRule> byEntityId;
    public List<String> blacklist;

    public static class Debug {
        public boolean renderBoxes = false;
        public boolean renderVanillaBox = true;
        public boolean renderTargetingBox = false;
        public float maxRenderDistance = 48.0F;
        public boolean logRuleResolution = false;
    }

    /** The profile currently in use (never null). */
    public HitboxProfile active() {
        if (profiles == null || profiles.isEmpty()) {
            profiles = HitboxProfile.presets();
        }
        HitboxProfile profile = profiles.get(activeProfile);
        if (profile == null) {
            activeProfile = profiles.keySet().iterator().next();
            profile = profiles.get(activeProfile);
        }
        return profile;
    }

    public void sanitize() {
        if (maxScale <= 0.0F) maxScale = 8.0F;
        maxScale = Math.min(maxScale, 64.0F);
        if (debug == null) debug = new Debug();
        if (profiles == null || profiles.isEmpty()) profiles = HitboxProfile.presets();

        migrateLegacy();

        if (activeProfile == null || !profiles.containsKey(activeProfile)) {
            activeProfile = profiles.keySet().iterator().next();
        }
        profiles.values().removeIf(java.util.Objects::isNull);
        profiles.values().forEach(HitboxProfile::sanitize);
    }

    /** Moves an old flat config (rules at the root) into a profile named "migrated". */
    private void migrateLegacy() {
        boolean hasLegacy = defaults != null || selfPlayer != null || otherPlayers != null
                || byCategory != null || byEntityId != null || blacklist != null;
        if (!hasLegacy) return;

        HitboxProfile migrated = new HitboxProfile();
        migrated.description = "Imported from a pre-profile config";
        if (defaults != null) migrated.defaults = defaults;
        if (selfPlayer != null) migrated.selfPlayer = selfPlayer;
        if (otherPlayers != null) migrated.otherPlayers = otherPlayers;
        if (byCategory != null) migrated.byCategory = new LinkedHashMap<>(byCategory);
        if (byEntityId != null) migrated.byEntityId = new LinkedHashMap<>(byEntityId);
        if (blacklist != null) migrated.blacklist = new ArrayList<>(blacklist);
        migrated.sanitize();

        profiles.put("migrated", migrated);
        activeProfile = "migrated";

        defaults = null;
        selfPlayer = null;
        otherPlayers = null;
        byCategory = null;
        byEntityId = null;
        blacklist = null;
    }
}
