package com.letters.hitboxes.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A complete, switchable set of rules. Profiles let you flip between setups instantly
 * (e.g. "default" for normal play, "huge" for testing, "off" for vanilla).
 */
public class HitboxProfile {

    public String description = "";
    public ScaleRule defaults = new ScaleRule(HitboxMode.DIMENSIONS, 1.25F, 1.0F);
    public ScaleRule selfPlayer = new ScaleRule(HitboxMode.OFF, 1.0F, 1.0F);
    public ScaleRule otherPlayers = new ScaleRule(HitboxMode.DIMENSIONS, 1.25F, 1.0F);
    public Map<String, ScaleRule> byCategory = new LinkedHashMap<>();
    public Map<String, ScaleRule> byEntityId = new LinkedHashMap<>();
    public List<String> blacklist = new ArrayList<>();
    public VisualSettings visual = new VisualSettings();

    public void sanitize() {
        if (defaults == null) defaults = new ScaleRule(HitboxMode.DIMENSIONS, 1.0F, 1.0F);
        if (defaults.mode == null) defaults.mode = HitboxMode.DIMENSIONS;
        if (defaults.width == null) defaults.width = 1.0F;
        if (defaults.height == null) defaults.height = 1.0F;
        if (defaults.eyeHeight == null) defaults.eyeHeight = 1.0F;
        if (defaults.pickRadiusBonus == null) defaults.pickRadiusBonus = 0.0F;
        if (defaults.widthBonus == null) defaults.widthBonus = 0.0F;
        if (defaults.heightBonus == null) defaults.heightBonus = 0.0F;
        if (defaults.scaleFixedDimensions == null) defaults.scaleFixedDimensions = false;
        if (selfPlayer == null) selfPlayer = new ScaleRule(HitboxMode.OFF, 1.0F, 1.0F);
        if (otherPlayers == null) otherPlayers = new ScaleRule(HitboxMode.OFF, 1.0F, 1.0F);
        if (byCategory == null) byCategory = new LinkedHashMap<>();
        if (byEntityId == null) byEntityId = new LinkedHashMap<>();
        if (blacklist == null) blacklist = new ArrayList<>();
        if (visual == null) visual = new VisualSettings();
        visual.sanitize();
    }

    public HitboxProfile copy() {
        HitboxProfile out = new HitboxProfile();
        out.description = description;
        out.defaults = ScaleRule.inherit(defaults, new ScaleRule());
        out.selfPlayer = ScaleRule.inherit(selfPlayer, new ScaleRule());
        out.otherPlayers = ScaleRule.inherit(otherPlayers, new ScaleRule());
        out.byCategory = new LinkedHashMap<>();
        byCategory.forEach((k, v) -> out.byCategory.put(k, ScaleRule.inherit(v, new ScaleRule())));
        out.byEntityId = new LinkedHashMap<>();
        byEntityId.forEach((k, v) -> out.byEntityId.put(k, ScaleRule.inherit(v, new ScaleRule())));
        out.blacklist = new ArrayList<>(blacklist);
        VisualSettings v = new VisualSettings();
        v.scaleModels = visual.scaleModels;
        v.scaleSelfModel = visual.scaleSelfModel;
        v.modelWidthFactor = visual.modelWidthFactor;
        v.modelHeightFactor = visual.modelHeightFactor;
        v.maxModelScale = visual.maxModelScale;
        v.scaleShadow = visual.scaleShadow;
        out.visual = v;
        return out;
    }

    // ------------------------------------------------------------------ presets

    private static final List<String> DEFAULT_BLACKLIST = List.of(
            "minecraft:ender_dragon", "minecraft:falling_block", "minecraft:tnt", "minecraft:end_crystal");

    /** Balanced: mobs a bit wider, own player untouched. */
    public static HitboxProfile presetDefault() {
        HitboxProfile p = new HitboxProfile();
        p.description = "Balanced: wider mobs, own player untouched";
        p.defaults = new ScaleRule(HitboxMode.DIMENSIONS, 1.25F, 1.0F);
        p.selfPlayer = new ScaleRule(HitboxMode.OFF, 1.0F, 1.0F);
        p.otherPlayers = new ScaleRule(HitboxMode.DIMENSIONS, 1.25F, 1.0F);
        p.byCategory.put("monster", new ScaleRule(HitboxMode.DIMENSIONS, 1.4F, 1.0F));
        p.byCategory.put("creature", new ScaleRule(HitboxMode.DIMENSIONS, 1.25F, 1.0F));
        p.byCategory.put("ambient", new ScaleRule(HitboxMode.DIMENSIONS, 1.5F, 1.0F));
        p.byCategory.put("water_creature", new ScaleRule(HitboxMode.DIMENSIONS, 1.25F, 1.0F));
        p.byCategory.put("water_ambient", new ScaleRule(HitboxMode.DIMENSIONS, 1.5F, 1.0F));
        p.byCategory.put("underground_water_creature", new ScaleRule(HitboxMode.DIMENSIONS, 1.25F, 1.0F));
        p.byCategory.put("axolotls", new ScaleRule(HitboxMode.DIMENSIONS, 1.25F, 1.0F));
        p.byCategory.put("misc", new ScaleRule(HitboxMode.OFF, 1.0F, 1.0F));
        ScaleRule creeper = new ScaleRule();
        creeper.width = 1.6F;
        p.byEntityId.put("minecraft:creeper", creeper);
        p.blacklist = new ArrayList<>(DEFAULT_BLACKLIST);
        return p;
    }

    /** Everything vanilla - handy as a quick "compare" profile. */
    public static HitboxProfile presetOff() {
        HitboxProfile p = new HitboxProfile();
        p.description = "Vanilla hitboxes (nothing changed)";
        p.defaults = new ScaleRule(HitboxMode.OFF, 1.0F, 1.0F);
        p.selfPlayer = new ScaleRule(HitboxMode.OFF, 1.0F, 1.0F);
        p.otherPlayers = new ScaleRule(HitboxMode.OFF, 1.0F, 1.0F);
        p.visual.scaleModels = false;
        p.blacklist = new ArrayList<>(DEFAULT_BLACKLIST);
        return p;
    }

    /** Very obvious - good for seeing that the change is physical, not visual. */
    public static HitboxProfile presetHuge() {
        HitboxProfile p = new HitboxProfile();
        p.description = "Double size mobs, models follow the hitbox";
        p.defaults = new ScaleRule(HitboxMode.DIMENSIONS, 2.0F, 1.5F);
        p.selfPlayer = new ScaleRule(HitboxMode.OFF, 1.0F, 1.0F);
        p.otherPlayers = new ScaleRule(HitboxMode.DIMENSIONS, 1.5F, 1.0F);
        p.byCategory.put("misc", new ScaleRule(HitboxMode.OFF, 1.0F, 1.0F));
        p.blacklist = new ArrayList<>(DEFAULT_BLACKLIST);
        return p;
    }

    /** Physics stays vanilla, only the ray-cast box grows (easier aiming in singleplayer). */
    public static HitboxProfile presetTargeting() {
        HitboxProfile p = new HitboxProfile();
        p.description = "Vanilla physics, only crosshair/projectile ray-cast box is bigger";
        ScaleRule rule = new ScaleRule(HitboxMode.TARGETING, 1.0F, 1.0F);
        rule.pickRadiusBonus = 0.35F;
        p.defaults = rule;
        p.selfPlayer = new ScaleRule(HitboxMode.OFF, 1.0F, 1.0F);
        ScaleRule players = new ScaleRule(HitboxMode.TARGETING, 1.0F, 1.0F);
        players.pickRadiusBonus = 0.35F;
        p.otherPlayers = players;
        p.byCategory.put("misc", new ScaleRule(HitboxMode.OFF, 1.0F, 1.0F));
        p.visual.scaleModels = false;
        p.blacklist = new ArrayList<>(DEFAULT_BLACKLIST);
        return p;
    }

    public static Map<String, HitboxProfile> presets() {
        Map<String, HitboxProfile> map = new LinkedHashMap<>();
        map.put("default", presetDefault());
        map.put("huge", presetHuge());
        map.put("targeting", presetTargeting());
        map.put("off", presetOff());
        return map;
    }
}
