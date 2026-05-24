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
import org.joml.Matrix4f;
import top.huliawsl.blockwright.preview.PlannedBlock;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.selection.BoxRegionSelection;

public final class BlockwrightSelectionRenderer {
    private static final double CORNER_PADDING = 0.02D;
    private static final double REGION_PADDING = 0.002D;
    private static final double PREVIEW_INSET = 0.06D;

    private BlockwrightSelectionRenderer() {
    }

    public static void render(PoseStack poseStack, Camera camera) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        BoxRegionSelection regionSelection = ClientSelectionState.getRegionSelection();
        PreviewPlan previewPlan = ClientPreviewState.getPreviewPlan();
        if (regionSelection.getPos1() == null && regionSelection.getPos2() == null
                && (previewPlan == null || previewPlan.getPlannedBlocks().isEmpty())) {
            return;
        }

        Vec3 cameraPosition = camera.getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        VertexConsumer fillConsumer = bufferSource.getBuffer(RenderType.debugFilledBox());

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        renderPreview(previewPlan, poseStack, fillConsumer, lineConsumer);
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
        bufferSource.endBatch(RenderType.debugFilledBox());
        bufferSource.endBatch(RenderType.lines());
    }

    private static void renderPreview(PreviewPlan plan, PoseStack poseStack, VertexConsumer fillConsumer, VertexConsumer lineConsumer) {
        if (plan == null || plan.getPlannedBlocks().isEmpty()) {
            return;
        }

        float red = 0.42F;
        float green = 0.86F;
        float blue = 1.0F;
        if (plan.getOverallSeverity() == PreviewSeverity.WARNING) {
            red = 1.0F;
            green = 0.78F;
            blue = 0.32F;
        } else if (plan.getOverallSeverity() == PreviewSeverity.ERROR) {
            red = 1.0F;
            green = 0.32F;
            blue = 0.32F;
        }

        PoseStack.Pose pose = poseStack.last();
        for (PlannedBlock block : plan.getPlannedBlocks()) {
            BlockPos pos = block.getPos();
            renderFilledBox(
                    pose.pose(),
                    fillConsumer,
                    (float) (pos.getX() + PREVIEW_INSET),
                    (float) (pos.getY() + PREVIEW_INSET),
                    (float) (pos.getZ() + PREVIEW_INSET),
                    (float) (pos.getX() + 1 - PREVIEW_INSET),
                    (float) (pos.getY() + 1 - PREVIEW_INSET),
                    (float) (pos.getZ() + 1 - PREVIEW_INSET),
                    red,
                    green,
                    blue,
                    0.22F
            );
        }

        LevelRenderer.renderLineBox(
                poseStack,
                lineConsumer,
                plan.getBounds().inflate(REGION_PADDING),
                red,
                green,
                blue,
                0.9F
        );
    }

    private static void renderFilledBox(Matrix4f pose, VertexConsumer consumer,
                                        float minX, float minY, float minZ,
                                        float maxX, float maxY, float maxZ,
                                        float red, float green, float blue, float alpha) {
        addFace(consumer, pose, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        addFace(consumer, pose, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ, red, green, blue, alpha);
        addFace(consumer, pose, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        addFace(consumer, pose, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue, alpha);
        addFace(consumer, pose, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        addFace(consumer, pose, minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, red, green, blue, alpha);
    }

    private static void addFace(VertexConsumer consumer, Matrix4f pose,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float red, float green, float blue, float alpha) {
        addVertex(consumer, pose, x1, y1, z1, red, green, blue, alpha);
        addVertex(consumer, pose, x2, y2, z2, red, green, blue, alpha);
        addVertex(consumer, pose, x3, y3, z3, red, green, blue, alpha);
        addVertex(consumer, pose, x4, y4, z4, red, green, blue, alpha);
    }

    private static void addVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z,
                                  float red, float green, float blue, float alpha) {
        consumer.vertex(pose, x, y, z).color(red, green, blue, alpha).endVertex();
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
