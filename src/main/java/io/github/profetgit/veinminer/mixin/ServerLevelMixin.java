package io.github.profetgit.veinminer.mixin;

import io.github.profetgit.veinminer.server.Chain;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The chain plays its own break effects once per ring, so the per-block block-break event (2001) is dropped while it breaks a block. */
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    @Inject(method = "levelEvent(Lnet/minecraft/world/entity/Entity;ILnet/minecraft/core/BlockPos;I)V", at = @At("HEAD"), cancellable = true)
    private void veinminer$quiet(Entity source, int type, BlockPos pos, int data, CallbackInfo ci) {
        if (type == 2001 && Chain.quiet()) ci.cancel();
    }
}
