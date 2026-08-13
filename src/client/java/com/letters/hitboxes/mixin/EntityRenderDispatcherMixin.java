package com.letters.hitboxes.mixin;

import com.letters.hitboxes.HitboxEngine;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Visual scaling for NON-living entities (boats, minecarts, arrows, item entities...).
 * Living entities are handled by {@link LivingEntityRendererMixin}.
 *
 * <p>Only the {@code EntityRenderer#render} call is wrapped, so the shadow and the vanilla F3+B
 * hitbox outline keep using the real (already physical) bounding box instead of being scaled twice.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void lettersHitboxes$scaleNonLivingModel(EntityRenderer renderer, Entity entity, float yaw,
                                                    float partialTick, PoseStack poseStack,
                                                    MultiBufferSource buffers, int packedLight,
                                                    Operation<Void> original) {
        float[] scale = entity instanceof LivingEntity ? null : HitboxEngine.modelScale(entity);
        if (scale == null) {
            original.call(renderer, entity, yaw, partialTick, poseStack, buffers, packedLight);
            return;
        }
        poseStack.pushPose();
        poseStack.scale(scale[0], scale[1], scale[0]);
        original.call(renderer, entity, yaw, partialTick, poseStack, buffers, packedLight);
        poseStack.popPose();
    }
}
