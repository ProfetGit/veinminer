package io.github.profetgit.veinminer.client.render;

//? if >=26.2 {
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.profetgit.veinminer.core.StandKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Draws a chain stand-in as a moving block over the real level, the way a piston draws the block it moves. Terrain's own
 * mesher then lights it: the same face shading, ambient occlusion, neighbour light and tint as the chunk mesh that showed the
 * block a tick before, and under a shader pack the same program (Iris maps the moving-block pipelines to its terrain
 * program), so the swap from the real block shows no colour step. {@link ShaderPack} adds the block id Iris gives chunk
 * meshes, so an ore keeps its pack's glow.
 */
public final class StandDraw extends MovingBlockRenderState {
    private ClientLevel level;

    /**
     * @param origin the pose's origin: the block's corner (block display) or its centre (item display)
     * @param cell the mark of the stand-in (the cell it stands in, so the block is that far back)
     * @return false when there is no level or block (the vanilla path draws it)
     */
    public static boolean submit(BlockState block, double x, double y, double z, int mark, boolean centred, PoseStack poseStack,
                                 SubmitNodeCollector collector, int outlineColor) {
        ClientLevel level = Minecraft.getInstance().level;
        if (block == null || level == null || block.isAir()) return false;
        int[] n = StandKeys.cell(mark);
        BlockPos pos = BlockPos.containing(Math.round(x) - n[0] + 0.5, Math.round(y) - n[1] + 0.5, Math.round(z) - n[2] + 0.5);
        StandDraw s = new StandDraw();
        s.level = level;
        s.blockPos = pos;
        s.randomSeedPos = pos;
        s.blockState = block;
        s.biome = level.getBiome(pos);
        s.cardinalLighting = level.cardinalLighting();
        s.lightEngine = level.getLightEngine();
        poseStack.pushPose();
        if (centred) poseStack.translate(-0.5f, -0.5f, -0.5f);
        collector.submitMovingBlock(poseStack, s, outlineColor);
        poseStack.popPose();
        return true;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return pos.equals(this.blockPos) ? this.blockState : level.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver color) {
        return level.getBlockTint(pos, color);
    }

    @Override
    public int getHeight() {
        return level.getHeight();
    }

    @Override
    public int getMinY() {
        return level.getMinY();
    }
}
//?} else {
/*public final class StandDraw {
    private StandDraw() {}
}
*///?}
