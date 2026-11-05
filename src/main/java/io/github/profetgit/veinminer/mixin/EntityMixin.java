package io.github.profetgit.veinminer.mixin;

import io.github.profetgit.veinminer.server.Fx;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A chain's stand-ins and loot ghosts are never saved: a world that closes mid-chain loads without them. */
@Mixin(Entity.class)
abstract class EntityMixin {
    @Inject(method = "shouldBeSaved", at = @At("HEAD"), cancellable = true)
    private void veinminer$neverSave(CallbackInfoReturnable<Boolean> cir) {
        if (Fx.ours((Entity) (Object) this)) cir.setReturnValue(false);
    }
}
