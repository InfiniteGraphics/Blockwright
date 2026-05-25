package top.huliawsl.blockwright.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import top.huliawsl.blockwright.module.model.ModuleConnector;
import top.huliawsl.blockwright.module.model.ModuleDefinition;
import top.huliawsl.blockwright.pack.SpongeSchematicData;

public final class ModuleSchematicPreviewRenderer {
    private static final BlockState AIR_PREVIEW_STATE = Blocks.GLASS.defaultBlockState();
    private static final BlockState CONNECTOR_PREVIEW_STATE = Blocks.LIME_STAINED_GLASS.defaultBlockState();
    private static final BlockState BOUNDS_PREVIEW_STATE = Blocks.RED_STAINED_GLASS.defaultBlockState();

    private ModuleSchematicPreviewRenderer() {
    }

    public static void render(GuiGraphics guiGraphics, int x, int y, int width, int height,
                              ModuleDefinition module, int rotationQuarterTurns, int zoomPercent,
                              boolean showBounds, boolean showConnectors, boolean showAir) {
        guiGraphics.fill(x, y, x + width, y + height, 0x9010151A);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFF39424D);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF39424D);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFF39424D);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF39424D);

        if (module == null) {
            return;
        }
        SpongeSchematicData schematic = module.schematicData;
        if (schematic == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        float size = Math.max(schematic.getWidth(), Math.max(schematic.getHeight(), schematic.getLength()));
        float scale = Math.min(width, height) / Math.max(4.0F, size * 2.8F);
        scale *= Math.max(0.5F, zoomPercent / 100.0F);
        float centerX = x + width / 2.0F;
        float centerY = y + height - 12.0F;

        guiGraphics.flush();
        RenderSystem.enableDepthTest();
        Lighting.setupForFlatItems();

        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 180.0F);
        poseStack.scale(scale, -scale, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(-28.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F + rotationQuarterTurns * 90.0F));
        poseStack.translate(-schematic.getWidth() / 2.0F, -schematic.getHeight() / 2.0F, -schematic.getLength() / 2.0F);

        int fullBright = LightTexture.pack(15, 15);
        for (int yPos = 0; yPos < schematic.getHeight(); yPos++) {
            for (int zPos = 0; zPos < schematic.getLength(); zPos++) {
                for (int xPos = 0; xPos < schematic.getWidth(); xPos++) {
                    BlockState state = schematic.getBlockState(xPos, yPos, zPos);
                    if (state == null) {
                        continue;
                    }
                    if (state.isAir()) {
                        if (!showAir) {
                            continue;
                        }
                        state = AIR_PREVIEW_STATE;
                    }
                    if (state.getRenderShape() != RenderShape.MODEL) {
                        continue;
                    }
                    poseStack.pushPose();
                    poseStack.translate(xPos, yPos, zPos);
                    blockRenderer.renderSingleBlock(state, poseStack, bufferSource, fullBright, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                }
            }
        }

        if (showConnectors) {
            renderConnectors(blockRenderer, bufferSource, poseStack, module, fullBright);
        }
        if (showBounds) {
            renderBounds(blockRenderer, bufferSource, poseStack, schematic, fullBright);
        }

        bufferSource.endBatch();
        poseStack.popPose();
        Lighting.setupFor3DItems();
        RenderSystem.disableDepthTest();
    }

    private static void renderConnectors(BlockRenderDispatcher blockRenderer, MultiBufferSource.BufferSource bufferSource,
                                         com.mojang.blaze3d.vertex.PoseStack poseStack, ModuleDefinition module, int light) {
        for (ModuleConnector connector : module.connectors) {
            if (connector.offset.size() != 3 || connector.size.size() != 3) {
                continue;
            }
            int minX = connector.offset.get(0);
            int minY = connector.offset.get(1);
            int minZ = connector.offset.get(2);
            int maxX = minX + connector.size.get(0) - 1;
            int maxY = minY + connector.size.get(1) - 1;
            int maxZ = minZ + connector.size.get(2) - 1;
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (x != minX && x != maxX && y != minY && y != maxY && z != minZ && z != maxZ) {
                            continue;
                        }
                        poseStack.pushPose();
                        poseStack.translate(x, y, z);
                        blockRenderer.renderSingleBlock(CONNECTOR_PREVIEW_STATE, poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY);
                        poseStack.popPose();
                    }
                }
            }
        }
    }

    private static void renderBounds(BlockRenderDispatcher blockRenderer, MultiBufferSource.BufferSource bufferSource,
                                     com.mojang.blaze3d.vertex.PoseStack poseStack, SpongeSchematicData schematic, int light) {
        int maxX = schematic.getWidth() - 1;
        int maxY = schematic.getHeight() - 1;
        int maxZ = schematic.getLength() - 1;
        int[][] corners = new int[][] {
                {0, 0, 0},
                {maxX, 0, 0},
                {0, maxY, 0},
                {0, 0, maxZ},
                {maxX, maxY, 0},
                {maxX, 0, maxZ},
                {0, maxY, maxZ},
                {maxX, maxY, maxZ}
        };
        for (int[] corner : corners) {
            poseStack.pushPose();
            poseStack.translate(corner[0], corner[1], corner[2]);
            blockRenderer.renderSingleBlock(BOUNDS_PREVIEW_STATE, poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        BlockPos center = new BlockPos(Math.max(0, maxX / 2), Math.max(0, maxY / 2), Math.max(0, maxZ / 2));
        poseStack.pushPose();
        poseStack.translate(center.getX(), center.getY(), center.getZ());
        blockRenderer.renderSingleBlock(BOUNDS_PREVIEW_STATE, poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
