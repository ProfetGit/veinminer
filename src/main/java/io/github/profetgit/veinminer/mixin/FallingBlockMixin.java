package io.github.profetgit.veinminer.mixin;

import io.github.profetgit.veinminer.server.Chain;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A block a running chain has not reached yet stays where it is: sand and gravel whose support the chain broke do not fall mid-chain. */
@Mixin(FallingBlock.class)
abstract class FallingBlockMixin {
    @Inject(method = "tick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V", at = @At("HEAD"), cancellable = true)
    private void veinminer$hold(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (Chain.claimed(level, pos)) ci.cancel();
    }
}
