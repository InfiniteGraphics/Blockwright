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
import org.joml.Vector3f;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.selection.BoxRegionSelection;
import top.huliawsl.blockwright.selection.SplineSelection;
import net.minecraft.world.level.block.RenderShape;

public final class BlockwrightSelectionRenderer {
    private static final float PREVIEW_ALPHA = 0.4F;
    private static final double CORNER_PADDING = 0.02D;
    private static final double REGION_PADDING = 0.002D;
    private static final double SPLINE_POINT_PADDING = 0.12D;
    private static final double GIZMO_LENGTH = 2.0D;
    private static final double GIZMO_ARROW_LENGTH = 0.38D;
    private static final double GIZMO_ARROW_HALF_BASE = 0.12D;

    private BlockwrightSelectionRenderer() {
    }

    public static void render(PoseStack poseStack, Camera camera) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        BoxRegionSelection regionSelection = ClientSelectionState.getRegionSelection();
        SplineSelection splineSelection = ClientSelectionState.getSplineSelection();
        BlockPos hoverPlacement = ClientSelectionState.getHoverPlacement();
        PreviewPlan previewPlan = ClientPreviewState.getPreviewPlan();
        PcgEditorSession editorSession = PcgEditorSession.get();
        if (regionSelection.getPos1() == null && regionSelection.getPos2() == null
                && splineSelection.getPoints().isEmpty()
                && hoverPlacement == null
                && (previewPlan == null || previewPlan.getPlannedBlocks().isEmpty())) {
            return;
        }

        Vec3 cameraPosition = camera.getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        renderPreview(minecraft, previewPlan, poseStack, bufferSource);
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        renderCorner(lineConsumer, poseStack, regionSelection.getPos1(), 0.95F, 0.78F, 0.24F,
                editorSession.getSelection() == PcgEditorSelection.REGION && editorSession.getSelectedRegionCornerIndex() == 0);
        renderCorner(lineConsumer, poseStack, regionSelection.getPos2(), 0.24F, 0.78F, 0.95F,
                editorSession.getSelection() == PcgEditorSelection.REGION && editorSession.getSelectedRegionCornerIndex() == 1);
        renderSpline(lineConsumer, poseStack, splineSelection, editorSession);
        renderHoverPlacement(lineConsumer, poseStack, hoverPlacement);
        renderPreviewBounds(lineConsumer, poseStack, previewPlan);
        renderTransformGizmo(lineConsumer, poseStack, editorSession);

        if (regionSelection.isComplete()) {
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineConsumer,
                    regionSelection.toAabb().inflate(REGION_PADDING),
                    editorSession.getSelection() == PcgEditorSelection.REGION ? 0.52F : 0.34F,
                    editorSession.getSelection() == PcgEditorSelection.REGION ? 0.92F : 0.82F,
                    editorSession.getSelection() == PcgEditorSelection.REGION ? 0.98F : 0.92F,
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

    private static void renderPreviewBounds(VertexConsumer lineConsumer, PoseStack poseStack, PreviewPlan plan) {
        if (plan == null || plan.getPlannedBlocks().isEmpty()) {
            return;
        }
        float red;
        float green;
        float blue;
        if (plan.isStale() || plan.getOverallSeverity() == PreviewSeverity.ERROR) {
            red = 0.95F;
            green = 0.35F;
            blue = 0.35F;
        } else if (plan.getOverallSeverity() == PreviewSeverity.WARNING) {
            red = 0.95F;
            green = 0.84F;
            blue = 0.34F;
        } else {
            red = 0.76F;
            green = 0.92F;
            blue = 0.98F;
        }
        LevelRenderer.renderLineBox(poseStack, lineConsumer, plan.getBounds().inflate(REGION_PADDING), red, green, blue, 1.0F);
    }

    private static void renderSpline(VertexConsumer lineConsumer, PoseStack poseStack, SplineSelection splineSelection, PcgEditorSession session) {
        if (splineSelection.getPoints().isEmpty()) {
            return;
        }

        BlockPos previous = null;
        for (BlockPos point : splineSelection.getPoints()) {
            boolean selected = session.getSelection() == PcgEditorSelection.SPLINE_POINT
                    && point.equals(session.getSelectedSplinePoint());
            renderSplinePoint(lineConsumer, poseStack, point, selected);
            if (previous != null) {
                renderSplineSegment(lineConsumer, poseStack, previous, point);
            }
            previous = point;
        }
    }

    private static void renderCorner(VertexConsumer lineConsumer, PoseStack poseStack, BlockPos pos,
                                     float red, float green, float blue, boolean selected) {
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
                selected ? Math.min(1.0F, red + 0.10F) : red,
                selected ? Math.min(1.0F, green + 0.12F) : green,
                selected ? Math.min(1.0F, blue + 0.14F) : blue,
                1.0F
        );
    }

    private static void renderSplinePoint(VertexConsumer lineConsumer, PoseStack poseStack, BlockPos pos, boolean selected) {
        LevelRenderer.renderLineBox(
                poseStack,
                lineConsumer,
                new AABB(
                        pos.getX() + SPLINE_POINT_PADDING,
                        pos.getY() + SPLINE_POINT_PADDING,
                        pos.getZ() + SPLINE_POINT_PADDING,
                        pos.getX() + 1 - SPLINE_POINT_PADDING,
                        pos.getY() + 1 - SPLINE_POINT_PADDING,
                        pos.getZ() + 1 - SPLINE_POINT_PADDING
                ),
                selected ? 0.98F : 0.96F,
                selected ? 0.88F : 0.38F,
                selected ? 0.98F : 0.22F,
                1.0F
        );
    }

    private static void renderHoverPlacement(VertexConsumer lineConsumer, PoseStack poseStack, BlockPos pos) {
        if (pos == null) {
            return;
        }
        LevelRenderer.renderLineBox(
                poseStack,
                lineConsumer,
                new AABB(
                        pos.getX() + 0.05D,
                        pos.getY() + 0.05D,
                        pos.getZ() + 0.05D,
                        pos.getX() + 0.95D,
                        pos.getY() + 0.95D,
                        pos.getZ() + 0.95D
                ),
                0.98F,
                0.98F,
                0.56F,
                1.0F
        );
    }

    private static void renderSplineSegment(VertexConsumer lineConsumer, PoseStack poseStack, BlockPos from, BlockPos to) {
        Vec3 fromCenter = Vec3.atCenterOf(from);
        Vec3 toCenter = Vec3.atCenterOf(to);
        Vector3f normal = new Vector3f(
                (float) (toCenter.x - fromCenter.x),
                (float) (toCenter.y - fromCenter.y),
                (float) (toCenter.z - fromCenter.z)
        );
        if (normal.lengthSquared() == 0.0F) {
            return;
        }
        normal.normalize();
        PoseStack.Pose pose = poseStack.last();
        lineConsumer.vertex(pose.pose(), (float) fromCenter.x, (float) fromCenter.y, (float) fromCenter.z)
                .color(0.95F, 0.46F, 0.98F, 1.0F)
                .normal(pose.normal(), normal.x(), normal.y(), normal.z())
                .endVertex();
        lineConsumer.vertex(pose.pose(), (float) toCenter.x, (float) toCenter.y, (float) toCenter.z)
                .color(0.95F, 0.46F, 0.98F, 1.0F)
                .normal(pose.normal(), normal.x(), normal.y(), normal.z())
                .endVertex();
    }

    private static void renderTransformGizmo(VertexConsumer lineConsumer, PoseStack poseStack, PcgEditorSession session) {
        Vec3 focus = session.getSelectionFocus();
        if (!session.isOpen() || focus == null
                || (session.getActiveTool() != PcgEditorTool.TRANSFORM && session.getSelection() != PcgEditorSelection.PREVIEW
                && session.getSelection() != PcgEditorSelection.REGION && session.getSelection() != PcgEditorSelection.SPLINE_POINT)) {
            return;
        }
        drawArrowAxis(lineConsumer, poseStack, focus, PcgEditorAxis.X, 0.96F, 0.26F, 0.24F, session);
        drawArrowAxis(lineConsumer, poseStack, focus, PcgEditorAxis.Y, 0.34F, 0.95F, 0.38F, session);
        drawArrowAxis(lineConsumer, poseStack, focus, PcgEditorAxis.Z, 0.32F, 0.56F, 0.96F, session);
    }

    private static void drawArrowAxis(VertexConsumer lineConsumer, PoseStack poseStack, Vec3 origin, PcgEditorAxis axis,
                                      float red, float green, float blue, PcgEditorSession session) {
        boolean hovered = session.getHoveredGizmoAxis() == axis;
        boolean active = session.getActiveGizmoAxis() == axis;
        float axisRed = active ? 1.0F : hovered ? Math.min(1.0F, red + 0.28F) : red;
        float axisGreen = active ? 1.0F : hovered ? Math.min(1.0F, green + 0.28F) : green;
        float axisBlue = active ? 0.82F : hovered ? Math.min(1.0F, blue + 0.20F) : blue;
        Vec3 direction = new Vec3(axis.stepX(), axis.stepY(), axis.stepZ());
        Vec3 tip = origin.add(direction.scale(GIZMO_LENGTH));
        drawAxis(lineConsumer, poseStack, origin, tip, axisRed, axisGreen, axisBlue);
        drawArrowHead(lineConsumer, poseStack, axis, tip, axisRed, axisGreen, axisBlue);
    }

    private static void drawArrowHead(VertexConsumer lineConsumer, PoseStack poseStack, PcgEditorAxis axis, Vec3 tip,
                                      float red, float green, float blue) {
        Vec3 axisDirection = new Vec3(axis.stepX(), axis.stepY(), axis.stepZ());
        Vec3 baseCenter = tip.subtract(axisDirection.scale(GIZMO_ARROW_LENGTH));
        Vec3 perpA;
        Vec3 perpB;
        if (axis == PcgEditorAxis.X) {
            perpA = new Vec3(0.0D, GIZMO_ARROW_HALF_BASE, 0.0D);
            perpB = new Vec3(0.0D, 0.0D, GIZMO_ARROW_HALF_BASE);
        } else if (axis == PcgEditorAxis.Y) {
            perpA = new Vec3(GIZMO_ARROW_HALF_BASE, 0.0D, 0.0D);
            perpB = new Vec3(0.0D, 0.0D, GIZMO_ARROW_HALF_BASE);
        } else {
            perpA = new Vec3(GIZMO_ARROW_HALF_BASE, 0.0D, 0.0D);
            perpB = new Vec3(0.0D, GIZMO_ARROW_HALF_BASE, 0.0D);
        }
        Vec3 c1 = baseCenter.add(perpA).add(perpB);
        Vec3 c2 = baseCenter.add(perpA).subtract(perpB);
        Vec3 c3 = baseCenter.subtract(perpA).subtract(perpB);
        Vec3 c4 = baseCenter.subtract(perpA).add(perpB);
        drawAxis(lineConsumer, poseStack, c1, tip, red, green, blue);
        drawAxis(lineConsumer, poseStack, c2, tip, red, green, blue);
        drawAxis(lineConsumer, poseStack, c3, tip, red, green, blue);
        drawAxis(lineConsumer, poseStack, c4, tip, red, green, blue);
        drawAxis(lineConsumer, poseStack, c1, c2, red, green, blue);
        drawAxis(lineConsumer, poseStack, c2, c3, red, green, blue);
        drawAxis(lineConsumer, poseStack, c3, c4, red, green, blue);
        drawAxis(lineConsumer, poseStack, c4, c1, red, green, blue);
    }

    private static void drawAxis(VertexConsumer lineConsumer, PoseStack poseStack, Vec3 from, Vec3 to, float red, float green, float blue) {
        Vector3f normal = new Vector3f((float) (to.x - from.x), (float) (to.y - from.y), (float) (to.z - from.z));
        if (normal.lengthSquared() == 0.0F) {
            return;
        }
        normal.normalize();
        PoseStack.Pose pose = poseStack.last();
        lineConsumer.vertex(pose.pose(), (float) from.x, (float) from.y, (float) from.z)
                .color(red, green, blue, 1.0F)
                .normal(pose.normal(), normal.x(), normal.y(), normal.z())
                .endVertex();
        lineConsumer.vertex(pose.pose(), (float) to.x, (float) to.y, (float) to.z)
                .color(red, green, blue, 1.0F)
                .normal(pose.normal(), normal.x(), normal.y(), normal.z())
                .endVertex();
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
