package io.github.profetgit.veinminer.mixin;

//? if >=26.2 {
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.profetgit.veinminer.client.render.ShaderPack;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every moving-block quad gets its buffer here; a stand-in's buffer is given its Iris block id (ore glow). */
@Mixin(RenderTypeFeatureRenderer.class)
public abstract class RenderTypeFeatureRendererMixin {
    @Inject(method = "getVertexBuilder", require = 0, at = @At("RETURN"))
    private void veinminer$tag(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir) {
        ShaderPack.tag(cir.getReturnValue());
    }
}
//?} else {
/*import org.spongepowered.asm.mixin.Mixin;

@Mixin(net.minecraft.client.Minecraft.class)
public abstract class RenderTypeFeatureRendererMixin {}
*///?}
