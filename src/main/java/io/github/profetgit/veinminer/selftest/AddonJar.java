package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.server.Config;
import net.minecraft.server.MinecraftServer;

//? if >=1.21 {
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;

/** Scene {@code addonjar}: an installed Enchanted Veinminer jar (a ModTest addon) gates veins on its enchantment. */
final class AddonJar {
    private AddonJar() {}

    static void run(MinecraftServer s) {
        Rig.begin(s);
        Config.use(Rig.PLAIN);
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("enchanted_veinminer", "veinminer"));
        ItemStack enchanted = new ItemStack(Items.IRON_PICKAXE);
        enchanted.enchant(s.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), 1);
        Rig.eq("addon jar: plain pickaxe breaks one block", 1, mine(new ItemStack(Items.IRON_PICKAXE)));
        Rig.eq("addon jar: enchanted pickaxe mines the whole vein", 27, mine(enchanted));
        Rig.clear();
    }

    private static int mine(ItemStack tool) {
        Rig.clear();
        Rig.hold(tool);
        List<BlockPos> cells = Rig.cube(1, 1, 1, 3, 3, 3, Blocks.COAL_ORE);
        Rig.strike(Rig.at(2, 2, 2), true);
        int air = Rig.air(cells);
        Rig.grab();
        return air;
    }
}
//?} else {
/*final class AddonJar {
    private AddonJar() {}

    static void run(MinecraftServer s) {}
}
*///?}
