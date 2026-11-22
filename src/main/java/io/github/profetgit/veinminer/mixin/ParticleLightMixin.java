package io.github.profetgit.veinminer.mixin;

//? if >=26.2 {
import io.github.profetgit.veinminer.client.render.ChainLight;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Particle.class)
public abstract class ParticleLightMixin {
    @Shadow
    protected double x;
    @Shadow
    protected double y;
    @Shadow
    protected double z;

    @Inject(method = "getLightCoords", at = @At("RETURN"), cancellable = true)
    private void veinminer$chainLight(float a, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ChainLight.floor(x, y, z, cir.getReturnValueI()));
    }
}
//?} else {
/*import org.spongepowered.asm.mixin.Mixin;

@Mixin(net.minecraft.client.Minecraft.class)
public abstract class ParticleLightMixin {}
*///?}
