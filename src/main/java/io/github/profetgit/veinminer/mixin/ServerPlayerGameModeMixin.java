package io.github.profetgit.veinminer.mixin;

import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;

//? if !fabric {
/*import io.github.profetgit.veinminer.server.Hooks;
import java.util.ArrayDeque;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}

/**
 * The vein start on NeoForge and Forge: their break events fire before the block is removed and wear the tool before
 * removing it, so there is no match for Fabric's {@code PlayerBlockBreakEvents.AFTER}. The hook runs when
 * {@code destroyBlock} returns true and the block really changed. Fabric keeps AFTER; there this class is empty.
 */
@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {
    //? if !fabric {
    /*@Shadow protected ServerLevel level;
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
    *///?}
}
