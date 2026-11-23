package io.github.profetgit.veinminer.mixin;

//? if >=26.2 {
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.profetgit.veinminer.client.render.StandDraw;
import io.github.profetgit.veinminer.client.render.StandState;
import io.github.profetgit.veinminer.core.StandKeys;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.BlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An ore stand-in (an item display on the server, so a vanilla client keeps the ore glow) is drawn like the other stand-ins: as its block. */
@Mixin(DisplayRenderer.ItemDisplayRenderer.class)
public abstract class ItemDisplayRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Display$ItemDisplay;Lnet/minecraft/client/renderer/entity/state/ItemDisplayEntityRenderState;F)V", at = @At("TAIL"))
    private void veinminer$keepBlock(Display.ItemDisplay entity, ItemDisplayEntityRenderState state, float partialTicks, CallbackInfo ci) {
        Display.ItemDisplay.ItemRenderState item = entity.itemRenderState();
        ((StandState) state).veinminer$block(item != null && item.itemStack().getItem() instanceof BlockItem b ? b.getBlock().defaultBlockState() : null);
    }

    @Inject(method = "submitInner", at = @At("HEAD"), cancellable = true)
    private void veinminer$moving(ItemDisplayEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                                  float progress, CallbackInfo ci) {
        if (state.renderState == null || !StandKeys.isMark(state.renderState.glowColorOverride())) return;
        if (StandDraw.submit(((StandState) state).veinminer$block(), state.x, state.y, state.z, state.renderState.glowColorOverride(), true,
            poseStack, collector, state.outlineColor)) ci.cancel();
    }
}
//?} else {
/*import org.spongepowered.asm.mixin.Mixin;

@Mixin(net.minecraft.client.Minecraft.class)
public abstract class ItemDisplayRendererMixin {}
*///?}
