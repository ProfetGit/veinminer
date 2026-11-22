package io.github.profetgit.veinminer.client.render;

//? if >=26.2 {
import net.minecraft.world.level.block.state.BlockState;

public interface StandState {
    BlockState veinminer$block();

    void veinminer$block(BlockState state);
}
//?} else {
/*public interface StandState {}
*///?}
