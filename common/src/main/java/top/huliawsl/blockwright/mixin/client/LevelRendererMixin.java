package top.huliawsl.blockwright.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.huliawsl.blockwright.client.BlockwrightSelectionRenderer;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void blockwright$renderSelectionOutline(PoseStack poseStack, float partialTick, long finishNanoTime,
                                                    boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
                                                    LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        BlockwrightSelectionRenderer.render(poseStack, camera);
    }
}
