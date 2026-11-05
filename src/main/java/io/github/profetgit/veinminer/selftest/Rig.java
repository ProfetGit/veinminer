package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.server.Chain;
import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.Hooks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceKey;

/** The server-thread helpers of the vein scenes: a work volume above the player, blocks in, strikes, loot counted out. */
final class Rig {
    static MinecraftServer server;
    static ServerLevel level;
    static ServerPlayer player;
    static BlockPos base;
    static final int SIZE = 12;
    /** The defaults with the chain off: the scenes that count blocks right after a strike break the whole vein at once. */
    static final Config.Values PLAIN = Config.Values.DEFAULTS.animation(false);

    private Rig() {}

    static void begin(MinecraftServer s) {
        server = s;
        player = s.getPlayerList().getPlayers().get(0);
        level = player.level();
        base = player.blockPosition().offset(-6, 14, -6);
        clear();
    }

    static BlockPos at(int x, int y, int z) {
        return base.offset(x, y, z);
    }

    static AABB box() {
        return new AABB(base.getX() - 1, base.getY() - 1, base.getZ() - 1, base.getX() + SIZE + 1, base.getY() + SIZE + 1, base.getZ() + SIZE + 1);
    }

    static void set(BlockPos p, BlockState s) {
        level.setBlock(p, s, 2 | 16);
    }

    static void clear() {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) set(at(x, y, z), air);
            }
        }
        grab();
    }

    static List<BlockPos> cube(int x, int y, int z, int nx, int ny, int nz, Block block) {
        List<BlockPos> cells = new ArrayList<>();
        for (int dx = 0; dx < nx; dx++) {
            for (int dy = 0; dy < ny; dy++) {
                for (int dz = 0; dz < nz; dz++) {
                    BlockPos p = at(x + dx, y + dy, z + dz);
                    set(p, block.defaultBlockState());
                    cells.add(p);
                }
            }
        }
        return cells;
    }

    static int air(List<BlockPos> cells) {
        int n = 0;
        for (BlockPos p : cells) if (level.getBlockState(p).isAir()) n++;
        return n;
    }

    static void hold(ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    static ItemStack pick(int silk, int fortune) {
        ItemStack s = new ItemStack(Items.IRON_PICKAXE);
        if (silk > 0) s.enchant(enchant(Enchantments.SILK_TOUCH), silk);
        if (fortune > 0) s.enchant(enchant(Enchantments.FORTUNE), fortune);
        return s;
    }

    private static net.minecraft.core.Holder<Enchantment> enchant(ResourceKey<Enchantment> key) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /** Mines the struck block as the player's client would, then lets the vein (if the gates pass) run. */
    static boolean strike(BlockPos pos, boolean sneak) {
        player.setShiftKeyDown(sneak);
        boolean ok = player.gameMode.destroyBlock(pos);
        Hooks.drain();
        player.setShiftKeyDown(false);
        return ok;
    }

    /** Ticks every running chain, the way the server tick does, until none is left; returns the ticks it took. */
    static int settle() {
        int n = 0;
        while (Chain.count() > 0 && n < 400) {
            Chain.tickAll();
            n++;
        }
        return n;
    }

    /** The loot in the work volume: items by type and the XP of the orbs. The entities are removed. */
    record Loot(Map<Item, Integer> items, int xp) {
        int count(Item item) {
            return items.getOrDefault(item, 0);
        }

        int total() {
            return items.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    static Loot grab() {
        Map<Item, Integer> items = new HashMap<>();
        int xp = 0;
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box())) {
            items.merge(e.getItem().getItem(), e.getItem().getCount(), Integer::sum);
            e.discard();
        }
        for (ExperienceOrb o : level.getEntitiesOfClass(ExperienceOrb.class, box())) {
            xp += o.getValue();
            o.discard();
        }
        return new Loot(items, xp);
    }

    static void check(String name, boolean ok, String detail) {
        SelfTest.record(name, ok, detail);
    }

    static void eq(String name, long expected, long actual) {
        check(name, expected == actual, "expected " + expected + " got " + actual);
    }
}
