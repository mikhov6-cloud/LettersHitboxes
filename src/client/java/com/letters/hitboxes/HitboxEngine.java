package com.letters.hitboxes;

import com.letters.hitboxes.config.ConfigManager;
import com.letters.hitboxes.config.HitboxConfig;
import com.letters.hitboxes.config.HitboxProfile;
import com.letters.hitboxes.config.ResolvedRule;
import com.letters.hitboxes.config.ScaleRule;
import com.letters.hitboxes.config.VisualSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime brain of the mod: resolves the rule for an entity (cached per entity type), builds the
 * modified {@link EntityDimensions} and provides the matching model scale for the renderer.
 * Everything here is client side only.
 */
public final class HitboxEngine {

    private static final Map<EntityType<?>, ResolvedRule> TYPE_CACHE = new ConcurrentHashMap<>();
    private static volatile ResolvedRule selfCache;
    private static volatile ResolvedRule othersCache;
    private static final float MIN_SIZE = 0.05F;
    private static final float MAX_SIZE = 64.0F;

    private HitboxEngine() {
    }

    /** Drop every cached rule and (optionally) rebuild the bounding boxes of all loaded entities. */
    public static void invalidate() {
        TYPE_CACHE.clear();
        selfCache = null;
        othersCache = null;
        if (ConfigManager.get().autoRefreshEntities) {
            refreshLoadedEntities();
        }
    }

    /** Forces every currently loaded entity to rebuild its bounding box from its dimensions. */
    public static void refreshLoadedEntities() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            if (mc.level == null) return;
            int count = 0;
            for (Entity entity : mc.level.entitiesForRendering()) {
                entity.refreshDimensions();
                count++;
            }
            if (mc.player != null) {
                mc.player.refreshDimensions();
            }
            LettersHitboxesClient.LOGGER.debug("[LettersHitboxes] refreshed {} entities", count);
        });
    }

    /** Master gate: config enabled + multiplayer policy. */
    public static boolean globallyActive() {
        HitboxConfig cfg = ConfigManager.get();
        if (!cfg.enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;
        // On a remote server the server keeps its own vanilla hitboxes, so this is opt-in.
        return cfg.applyOnMultiplayer || mc.hasSingleplayerServer();
    }

    public static ResolvedRule ruleFor(Entity entity) {
        if (entity == null || !globallyActive()) return ResolvedRule.NONE;
        HitboxConfig cfg = ConfigManager.get();
        HitboxProfile profile = cfg.active();

        if (entity instanceof Player) {
            Minecraft mc = Minecraft.getInstance();
            boolean self = mc.player != null && entity.getId() == mc.player.getId();
            ResolvedRule cached = self ? selfCache : othersCache;
            if (cached == null) {
                cached = ScaleRule.inherit(profile.defaults, self ? profile.selfPlayer : profile.otherPlayers)
                        .resolve(cfg.maxScale);
                if (self && cfg.protectSelfFromSuffocation && cached.height() > 1.0F) {
                    cached = new ResolvedRule(cached.mode(), cached.width(), 1.0F, cached.eyeHeight(),
                            cached.pickRadiusBonus(), cached.widthBonus(),
                            Math.min(cached.heightBonus(), 0.0F), cached.scaleFixedDimensions());
                }
                if (self) selfCache = cached;
                else othersCache = cached;
            }
            return cached;
        }

        return TYPE_CACHE.computeIfAbsent(entity.getType(), type -> resolveForType(cfg, profile, type));
    }

    private static ResolvedRule resolveForType(HitboxConfig cfg, HitboxProfile profile, EntityType<?> type) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        String id = key == null ? "unknown" : key.toString();

        if (isBlacklisted(profile, id)) {
            return ResolvedRule.NONE;
        }

        ScaleRule chain = profile.defaults;
        String category = type.getCategory().getName();
        chain = ScaleRule.inherit(chain, profile.byCategory.get(category));
        chain = ScaleRule.inherit(chain, profile.byEntityId.get(id));

        ResolvedRule resolved = chain.resolve(cfg.maxScale);
        if (cfg.debug.logRuleResolution) {
            LettersHitboxesClient.LOGGER.info("[LettersHitboxes] {} (category {}) -> {}", id, category, resolved);
        }
        return resolved;
    }

    private static boolean isBlacklisted(HitboxProfile profile, String id) {
        for (String entry : profile.blacklist) {
            if (entry == null || entry.isBlank()) continue;
            String trimmed = entry.trim();
            if (trimmed.endsWith("*")) {
                if (id.startsWith(trimmed.substring(0, trimmed.length() - 1))) return true;
            } else if (trimmed.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the physically modified dimensions, or {@code null} when nothing should change.
     * Called from the mixins on every {@code getDimensions} call.
     */
    public static EntityDimensions modifyDimensions(Entity entity, EntityDimensions base) {
        if (base == null) return null;
        ResolvedRule rule = ruleFor(entity);
        if (!rule.changesDimensions()) return null;

        float baseWidth = base.width();
        float baseHeight = base.height();
        float width = clampSize(baseWidth * rule.width() + rule.widthBonus());
        float height = clampSize(baseHeight * rule.height() + rule.heightBonus());
        if (width == baseWidth && height == baseHeight && rule.eyeHeight() == 1.0F) return null;

        // vanilla refuses to scale "fixed" dimensions (shulkers, item frames, ...) - we can force it
        if (base.fixed() && !rule.scaleFixedDimensions()) return null;

        float widthRatio = baseWidth > 0.0F ? width / baseWidth : 1.0F;
        float heightRatio = baseHeight > 0.0F ? height / baseHeight : 1.0F;
        float eyeHeight = base.eyeHeight() * heightRatio * rule.eyeHeight();

        EntityAttachments attachments = base.attachments().scale(widthRatio, heightRatio, widthRatio);
        return new EntityDimensions(width, height, eyeHeight, attachments, base.fixed());
    }

    /** Extra blocks added to the ray-cast box (crosshair targeting, arrows, tridents). */
    public static float extraPickRadius(Entity entity) {
        ResolvedRule rule = ruleFor(entity);
        return rule.changesTargeting() ? rule.pickRadiusBonus() : 0.0F;
    }

    /**
     * Model scale that matches the physical hitbox: {@code [horizontal, vertical]}, or {@code null}
     * when the model should stay vanilla. Used by the renderer mixin.
     */
    public static float[] modelScale(Entity entity) {
        if (entity == null || !globallyActive()) return null;
        VisualSettings visual = ConfigManager.get().active().visual;
        if (!visual.scaleModels) return null;

        Minecraft mc = Minecraft.getInstance();
        if (!visual.scaleSelfModel && mc != null && mc.player != null && entity.getId() == mc.player.getId()) {
            return null;
        }

        ResolvedRule rule = ruleFor(entity);
        if (!rule.changesDimensions()) return null;

        EntityDimensions base = entity.getType().getDimensions();
        float baseWidth = base.width();
        float baseHeight = base.height();
        if (baseWidth <= 0.0F || baseHeight <= 0.0F) return null;

        float width = clampSize(baseWidth * rule.width() + rule.widthBonus());
        float height = clampSize(baseHeight * rule.height() + rule.heightBonus());

        float horizontal = clampModel(width / baseWidth * visual.modelWidthFactor, visual.maxModelScale);
        float vertical = clampModel(height / baseHeight * visual.modelHeightFactor, visual.maxModelScale);
        if (Math.abs(horizontal - 1.0F) < 1.0E-4F && Math.abs(vertical - 1.0F) < 1.0E-4F) return null;

        return new float[]{horizontal, vertical};
    }

    private static float clampModel(float value, float max) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return 1.0F;
        return Math.max(0.05F, Math.min(value, max));
    }

    private static float clampSize(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return MIN_SIZE;
        return Math.max(MIN_SIZE, Math.min(value, MAX_SIZE));
    }
}
