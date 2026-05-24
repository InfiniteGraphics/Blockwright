package top.huliawsl.blockwright.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.selection.BoxRegionSelection;

public final class BlockwrightSelectionRenderer {
    private static final double CORNER_PADDING = 0.02D;
    private static final double REGION_PADDING = 0.002D;

    private BlockwrightSelectionRenderer() {
    }

    public static void render(PoseStack poseStack, Camera camera) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        BoxRegionSelection regionSelection = ClientSelectionState.getRegionSelection();
        if (regionSelection.getPos1() == null && regionSelection.getPos2() == null) {
            return;
        }

        Vec3 cameraPosition = camera.getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        renderCorner(lineConsumer, poseStack, regionSelection.getPos1(), 0.95F, 0.78F, 0.24F);
        renderCorner(lineConsumer, poseStack, regionSelection.getPos2(), 0.24F, 0.78F, 0.95F);

        if (regionSelection.isComplete()) {
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineConsumer,
                    regionSelection.toAabb().inflate(REGION_PADDING),
                    0.24F,
                    0.95F,
                    0.38F,
                    1.0F
            );
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }

    private static void renderCorner(VertexConsumer lineConsumer, PoseStack poseStack, BlockPos pos, float red, float green, float blue) {
        if (pos == null) {
            return;
        }
        LevelRenderer.renderLineBox(
                poseStack,
                lineConsumer,
                new AABB(
                        pos.getX() - CORNER_PADDING,
                        pos.getY() - CORNER_PADDING,
                        pos.getZ() - CORNER_PADDING,
                        pos.getX() + 1 + CORNER_PADDING,
                        pos.getY() + 1 + CORNER_PADDING,
                        pos.getZ() + 1 + CORNER_PADDING
                ),
                red,
                green,
                blue,
                1.0F
        );
    }
}
