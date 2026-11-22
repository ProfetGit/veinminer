package io.github.profetgit.veinminer.mixin;

//? if >=26.2 {
import io.github.profetgit.veinminer.client.render.StandState;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Keeps the displayed block state on the render state, so a stand-in can be drawn from the block's own model. */
@Mixin(BlockDisplayEntityRenderState.class)
public abstract class BlockDisplayStateMixin implements StandState {
    @Unique
    private BlockState veinminer$block;

    @Override
    public BlockState veinminer$block() {
        return veinminer$block;
    }

    @Override
    public void veinminer$block(BlockState state) {
        veinminer$block = state;
    }
}
//?} else {
/*import org.spongepowered.asm.mixin.Mixin;

@Mixin(net.minecraft.client.Minecraft.class)
public abstract class BlockDisplayStateMixin {}
*///?}
