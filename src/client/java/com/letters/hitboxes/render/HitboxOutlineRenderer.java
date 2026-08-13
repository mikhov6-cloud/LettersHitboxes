package com.letters.hitboxes.render;

import com.letters.hitboxes.HitboxEngine;
import com.letters.hitboxes.config.ConfigManager;
import com.letters.hitboxes.config.HitboxConfig;
import com.letters.hitboxes.config.ResolvedRule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Optional outline rendering so you can see what the physical hitbox is doing
 * (green = modified box, grey = vanilla box, orange = ray-cast box).
 */
public final class HitboxOutlineRenderer {

    private HitboxOutlineRenderer() {
    }

    public static void render(WorldRenderContext context) {
        HitboxConfig cfg = ConfigManager.get();
        if (!cfg.debug.renderBoxes) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || context.camera() == null) return;

        MultiBufferSource consumers = context.consumers();
        PoseStack poseStack = context.matrixStack();
        if (consumers == null || poseStack == null) return;

        VertexConsumer lines = consumers.getBuffer(RenderType.lines());
        Vec3 cam = context.camera().getPosition();
        double maxDistSq = (double) cfg.debug.maxRenderDistance * cfg.debug.maxRenderDistance;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.distanceToSqr(cam) > maxDistSq) continue;
            ResolvedRule rule = HitboxEngine.ruleFor(entity);
            if (rule.isNoop()) continue;

            AABB box = entity.getBoundingBox().move(-cam.x, -cam.y, -cam.z);

            if (rule.changesDimensions()) {
                LevelRenderer.renderLineBox(poseStack, lines, box, 0.25F, 1.0F, 0.35F, 1.0F);

                if (cfg.debug.renderVanillaBox) {
                    AABB vanilla = entity.getType().getDimensions()
                            .makeBoundingBox(entity.position())
                            .move(-cam.x, -cam.y, -cam.z);
                    LevelRenderer.renderLineBox(poseStack, lines, vanilla, 0.75F, 0.75F, 0.75F, 0.5F);
                }
            }

            if (cfg.debug.renderTargetingBox && rule.changesTargeting()) {
                LevelRenderer.renderLineBox(poseStack, lines,
                        box.inflate(entity.getPickRadius()), 1.0F, 0.6F, 0.1F, 0.9F);
            }
        }
    }
}
