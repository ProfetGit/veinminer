package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.Groups;
import io.github.profetgit.veinminer.server.Vein;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;

/** PRD 13 M1 {@code fair}: a vein loses and costs exactly what mining every block by hand would. */
final class Fair {
    private static final int VEINS = 500;
    private static final Identifier COAL_SEQUENCE = Identifier.withDefaultNamespace("blocks/coal_ore");

    private Fair() {}

    static void run(MinecraftServer server) {
        Rig.begin(server);
        Config.use(Config.Values.DEFAULTS);
        silk64();
        fortune();
        wear();
        near();
        durabilityOff();
        cap();
        noDrops();
        Config.use(Config.Values.DEFAULTS);
        Rig.clear();
    }

    private static void silk64() {
        Rig.clear();
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(1, 1, 1, 4, 4, 4, Blocks.COAL_ORE);
        Rig.strike(Rig.at(2, 2, 2), true);
        Rig.eq("silk touch vein of 64: air", 64, Rig.air(cells));
        Rig.Loot loot = Rig.grab();
        Rig.eq("silk touch vein of 64: coal_ore items", 64, loot.count(Items.COAL_ORE));
        Rig.eq("silk touch vein of 64: no other items", 0, loot.total() - loot.count(Items.COAL_ORE));
    }

    /** The same 8-coal cube, the same loot dice and XP dice, once as a vein and once block by block in the vein's order. */
    private static void fortune() {
        long veinItems = 0, veinXp = 0, handItems = 0, handXp = 0, veinWear = 0, handWear = 0, veinAir = 0;
        for (int i = 0; i < VEINS; i++) {
            long seed = 7919L * (i + 1);
            for (boolean vein : new boolean[]{true, false}) {
                Rig.clear();
                ItemStack pick = Rig.pick(0, 3);
                Rig.hold(pick);
                List<BlockPos> cells = Rig.cube(1, 1, 1, 2, 2, 2, Blocks.COAL_ORE);
                Rig.server.getRandomSequences().reset(COAL_SEQUENCE, seed);
                Rig.level.getRandom().setSeed(seed);
                BlockPos origin = Rig.at(1, 1, 1);
                if (vein) {
                    Rig.strike(origin, true);
                    veinAir += Rig.air(cells);
                } else {
                    Rig.strike(origin, false);
                    for (BlockPos p : Vein.scan(Rig.level, Rig.player.position(), origin, "coal", 7, 4096, true)) Rig.player.gameMode.destroyBlock(p);
                }
                Rig.Loot loot = Rig.grab();
                if (vein) {
                    veinItems += loot.count(Items.COAL);
                    veinXp += loot.xp();
                    veinWear += pick.getDamageValue();
                } else {
                    handItems += loot.count(Items.COAL);
                    handXp += loot.xp();
                    handWear += pick.getDamageValue();
                }
            }
        }
        Rig.eq("fortune III: every vein breaks all 8", 8L * VEINS, veinAir);
        Rig.eq("fortune III: tool wear equals by hand", handWear, veinWear);
        Rig.eq("fortune III: wear is 8 per vein", 8L * VEINS, veinWear);
        double itemRatio = handItems == 0 ? 0 : (double) veinItems / handItems;
        double xpRatio = handXp == 0 ? 0 : (double) veinXp / handXp;
        String detail = "items vein " + veinItems + " hand " + handItems + " (mean " + (double) veinItems / VEINS + " vs " + (double) handItems / VEINS
            + "), xp vein " + veinXp + " hand " + handXp;
        Rig.check("fortune III: mean items within 3% of by hand", handItems > 0 && Math.abs(itemRatio - 1) <= 0.03, detail);
        Rig.check("fortune III: mean xp within 3% of by hand", handXp > 0 && Math.abs(xpRatio - 1) <= 0.03, detail);
    }

    private static void wear() {
        Rig.clear();
        ItemStack pick = Rig.pick(0, 0);
        Rig.hold(pick);
        List<BlockPos> cells = Rig.cube(1, 1, 1, 4, 4, 4, Blocks.COAL_ORE);
        Rig.strike(Rig.at(2, 2, 2), true);
        Rig.eq("wear: vein of 64 costs 64", Rig.air(cells), pick.getDamageValue());
        Rig.eq("wear: all 64 broke", 64, Rig.air(cells));
        Rig.grab();
    }

    private static void near() {
        Rig.clear();
        ItemStack pick = Rig.pick(0, 0);
        pick.setDamageValue(pick.getMaxDamage() - 5);
        Rig.hold(pick);
        List<BlockPos> cells = Rig.cube(1, 1, 1, 4, 4, 4, Blocks.COAL_ORE);
        Rig.strike(Rig.at(2, 2, 2), true);
        int extras = Rig.air(cells) - 1;
        int left = pick.getMaxDamage() - pick.getDamageValue();
        Rig.check("worn pickaxe: at most 4 extra blocks", extras >= 1 && extras <= 4, "extras " + extras);
        Rig.check("worn pickaxe survives", !Rig.player.getMainHandItem().isEmpty() && Rig.player.getMainHandItem() == pick && left >= 1, "left " + left);
        Rig.check("worn pickaxe: rest stays ore", Rig.air(cells) < 64, "air " + Rig.air(cells));
        Rig.grab();
    }

    private static void durabilityOff() {
        Rig.clear();
        Config.use(Config.Values.DEFAULTS.durability(false));
        ItemStack pick = Rig.pick(0, 0);
        Rig.hold(pick);
        List<BlockPos> cells = Rig.cube(1, 1, 1, 2, 2, 2, Blocks.COAL_ORE);
        Rig.strike(Rig.at(1, 1, 1), true);
        Rig.eq("durability off: vein broke", 8, Rig.air(cells));
        Rig.eq("durability off: only the struck block cost", 1, pick.getDamageValue());
        Config.use(Config.Values.DEFAULTS);
        Rig.grab();
    }

    private static void cap() {
        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> cells = Rig.cube(1, 1, 1, 5, 7, 2, Blocks.COAL_ORE);
        Rig.strike(Rig.at(3, 3, 1), true);
        Rig.eq("max_blocks 64: a body of 70 loses 64", 64, Rig.air(cells));
        Rig.grab();
    }

    private static void noDrops() {
        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        Rig.level.getGameRules().set(GameRules.BLOCK_DROPS, false, Rig.server);
        List<BlockPos> cells = Rig.cube(1, 1, 1, 3, 3, 1, Blocks.COAL_ORE);
        Rig.strike(Rig.at(2, 2, 1), true);
        Rig.Loot loot = Rig.grab();
        Rig.level.getGameRules().set(GameRules.BLOCK_DROPS, true, Rig.server);
        Rig.eq("block_drops off: blocks gone", 9, Rig.air(cells));
        Rig.eq("block_drops off: no loot", 0, loot.total());
    }
}
