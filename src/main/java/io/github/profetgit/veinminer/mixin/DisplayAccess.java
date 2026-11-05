package io.github.profetgit.veinminer.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The private Display setters the chain poses its stand-ins and ghosts with (Display.java:231-328). */
@Mixin(Display.class)
public interface DisplayAccess {
    @Invoker("setTransformation")
    void veinminer$setTransformation(Transformation transformation);

    @Invoker("setTransformationInterpolationDuration")
    void veinminer$setInterpolationDuration(int duration);

    /** Always resends (entityData.set with force), which restarts the client's interpolation. */
    @Invoker("setTransformationInterpolationDelay")
    void veinminer$setInterpolationDelay(int ticks);

    @Invoker("setPosRotInterpolationDuration")
    void veinminer$setTeleportDuration(int duration);

    @Invoker("createTransformation")
    static Transformation veinminer$createTransformation(net.minecraft.network.syncher.SynchedEntityData data) {
        throw new AssertionError();
    }
}
