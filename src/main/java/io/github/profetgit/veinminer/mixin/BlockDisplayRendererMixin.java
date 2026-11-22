package io.github.profetgit.veinminer.mixin;

//? if >=26.2 {
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.profetgit.veinminer.client.render.StandShade;
import io.github.profetgit.veinminer.core.StandKeys;
import io.github.profetgit.veinminer.client.render.StandState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A chain stand-in is drawn with terrain's face shading instead of the entity lighting, so the swap from the real block shows no colour step. */
@Mixin(DisplayRenderer.BlockDisplayRenderer.class)
public abstract class BlockDisplayRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Display$BlockDisplay;Lnet/minecraft/client/renderer/entity/state/BlockDisplayEntityRenderState;F)V", at = @At("TAIL"))
    private void veinminer$keepBlock(Display.BlockDisplay entity, BlockDisplayEntityRenderState state, float partialTicks, CallbackInfo ci) {
        Display.BlockDisplay.BlockRenderState block = entity.blockRenderState();
        ((StandState) state).veinminer$block(block == null ? null : block.blockState());
    }

    @Inject(method = "submitInner", at = @At("HEAD"), cancellable = true)
    private void veinminer$terrainShade(BlockDisplayEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                                        float progress, CallbackInfo ci) {
        if (state.renderState == null || state.renderState.glowColorOverride() != StandKeys.MARK) return;
        if (StandShade.submit(((StandState) state).veinminer$block(), poseStack, collector, lightCoords)) ci.cancel();
    }
}
//?} else {
/*import org.spongepowered.asm.mixin.Mixin;

@Mixin(net.minecraft.client.Minecraft.class)
public abstract class BlockDisplayRendererMixin {}
*///?}
