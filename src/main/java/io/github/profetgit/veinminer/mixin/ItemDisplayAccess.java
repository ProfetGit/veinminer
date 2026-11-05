package io.github.profetgit.veinminer.mixin;

import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.ItemDisplay.class)
public interface ItemDisplayAccess {
    @Invoker("setItemStack")
    void veinminer$setItemStack(ItemStack stack);

    @Invoker("setItemTransform")
    void veinminer$setItemTransform(ItemDisplayContext context);
}
