package top.huliawsl.blockwright.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.selection.BoxRegionSelection;
import net.minecraft.world.level.block.RenderShape;

public final class BlockwrightSelectionRenderer {
    private static final float PREVIEW_ALPHA = 0.4F;
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
        PreviewPlan previewPlan = ClientPreviewState.getPreviewPlan();
        if (regionSelection.getPos1() == null && regionSelection.getPos2() == null
                && (previewPlan == null || previewPlan.getPlannedBlocks().isEmpty())) {
            return;
        }

        Vec3 cameraPosition = camera.getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        renderPreview(minecraft, previewPlan, poseStack, bufferSource);
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
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
        bufferSource.endBatch(RenderType.translucentMovingBlock());
    }

    private static void renderPreview(Minecraft minecraft, PreviewPlan plan, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
        if (plan == null || plan.getPlannedBlocks().isEmpty()) {
            return;
        }

        if (minecraft.level == null) {
            return;
        }

        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        GhostBufferSource ghostBufferSource = new GhostBufferSource(bufferSource, PREVIEW_ALPHA);
        for (var block : plan.getPlannedBlocks()) {
            if (block.getState().isAir() || block.getState().getRenderShape() != RenderShape.MODEL) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(block.getPos().getX(), block.getPos().getY(), block.getPos().getZ());
            blockRenderer.renderSingleBlock(
                    block.getState(),
                    poseStack,
                    ghostBufferSource,
                    LevelRenderer.getLightColor(minecraft.level, block.getState(), block.getPos()),
                    OverlayTexture.NO_OVERLAY
            );
            poseStack.popPose();
        }
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

    private static final class GhostBufferSource implements MultiBufferSource {
        private final MultiBufferSource delegate;
        private final float alpha;

        private GhostBufferSource(MultiBufferSource delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return new AlphaVertexConsumer(delegate.getBuffer(RenderType.translucentMovingBlock()), alpha);
        }
    }

    private static final class AlphaVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alpha;

        private AlphaVertexConsumer(VertexConsumer delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            delegate.color(red, green, blue, scaleAlpha(alpha));
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            delegate.defaultColor(red, green, blue, scaleAlpha(alpha));
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }

        @Override
        public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light,
                           float normalX, float normalY, float normalZ) {
            delegate.vertex(x, y, z, red, green, blue, alpha * this.alpha, u, v, overlay, light, normalX, normalY, normalZ);
        }

        private int scaleAlpha(int alpha) {
            return Math.max(0, Math.min(255, Math.round(alpha * this.alpha)));
        }
    }
}
