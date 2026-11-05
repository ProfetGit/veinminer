package io.github.profetgit.veinminer.mixin.forge;

import io.github.profetgit.veinminer.server.Hooks;
import java.util.ArrayDeque;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The vein start on Forge 1.x: the common ServerPlayerGameModeMixin is empty in the Fabric target's sources this side
 * project compiles, so the same hook lives here. It runs when destroyBlock returns true and the block really changed.
 */
@Mixin(ServerPlayerGameMode.class)
abstract class ForgeChopMixin {
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;

    // a stack, since another mod may break blocks from inside a break
    @Unique private final ArrayDeque<Object[]> veinminer$broken = new ArrayDeque<>();

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void veinminer$before(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        veinminer$broken.push(new Object[] {level.getBlockState(pos), level.getBlockEntity(pos)});
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void veinminer$after(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Object[] b = veinminer$broken.poll();
        if (b != null && cir.getReturnValueZ() && level.getBlockState(pos) != b[0]) Hooks.blockBroken(level, player, pos, (BlockState) b[0], (BlockEntity) b[1]);
    }
}
