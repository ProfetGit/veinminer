package io.github.profetgit.veinminer.mixin;

//? if >=26.2 {
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.profetgit.veinminer.client.render.ShaderPack;
import io.github.profetgit.veinminer.client.render.StandDraw;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.feature.MovingBlockFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Marks the tesselation of a stand-in, so its quads' buffers get the shader pack's block id. */
@Mixin(MovingBlockFeatureRenderer.class)
public abstract class MovingBlockFeatureRendererMixin {
    @WrapOperation(method = "buildGroup", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V"))
    private void veinminer$stand(ModelBlockRenderer renderer, BlockQuadOutput output, float x, float y, float z, BlockAndTintGetter level, BlockPos pos,
                                 BlockState state, BlockStateModel model, long seed, Operation<Void> original) {
        if (level instanceof StandDraw) ShaderPack.tagging(state, pos);
        try {
            original.call(renderer, output, x, y, z, level, pos, state, model, seed);
        } finally {
            ShaderPack.tagging(null, null);
        }
    }
}
//?} else {
/*import org.spongepowered.asm.mixin.Mixin;

@Mixin(net.minecraft.client.Minecraft.class)
public abstract class MovingBlockFeatureRendererMixin {}
*///?}
