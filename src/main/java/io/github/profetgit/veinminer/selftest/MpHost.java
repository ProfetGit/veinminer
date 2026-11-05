package io.github.profetgit.veinminer.selftest;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * The M4 multiplayer case {@code veinminer} (mode {@code mp-host}, dev/modtest.json): this client joins a dedicated
 * server, the case's steps op it, give it haste and an iron pickaxe and plant a 4x4x4 coal blob next to it, and it
 * mines one block of it for real ({@code startDestroyBlock}, so the server's break path). The server runs the chain
 * with {@code animation} on; this client counts the block displays the server sends it, the same replication the passive
 * Guest receives.
 */
final class MpHost {
    private static final BlockPos NEAR = new BlockPos(4, -60, 2);
    private static int maxDisplays;
    private static boolean sawStandIns;

    private MpHost() {}

    private static void cmd(Minecraft mc, String line) {
        mc.player.connection.sendCommand(line);
    }

    private static boolean joined(Minecraft mc) {
        return mc.player != null && mc.level != null && mc.getSingleplayerServer() == null && mc.gui.screen() == null;
    }

    private static int standIns(Minecraft mc) {
        int n = 0;
        for (Entity e : mc.level.entitiesForRendering()) if (e instanceof Display.BlockDisplay) n++;
        return n;
    }

    private static boolean blobGone(Minecraft mc) {
        for (int x = 3; x <= 6; x++) {
            for (int y = -60; y <= -57; y++) {
                for (int z = 2; z <= 5; z++) if (!mc.level.getBlockState(new BlockPos(x, y, z)).isAir()) return false;
            }
        }
        return true;
    }

    static Script build() {
        Script s = new Script();
        s.until("joined a server", 6000, MpHost::joined)
            .until("op, haste, pickaxe and the coal in view", 6000, mc -> mc.player.hasEffect(MobEffects.HASTE)
                && mc.player.getMainHandItem().is(Items.IRON_PICKAXE) && mc.level.getBlockState(NEAR).is(Blocks.COAL_ORE))
            .waitTicks(60)
            .run(mc -> SelfTest.record("mp ready", true, "haste, iron pickaxe, 64 coal in front"))
            .run(mc -> mc.gameMode.startDestroyBlock(NEAR, Direction.NORTH))
            .until("the vein to break on the server", 1200, mc -> {
                int n = standIns(mc);
                maxDisplays = Math.max(maxDisplays, n);
                sawStandIns |= n > 0;
                return blobGone(mc);
            })
            .waitTicks(60)
            .run(mc -> {
                SelfTest.record("mp vein broke", blobGone(mc), "64 blocks gone on this client");
                SelfTest.record("mp stand-ins reached the client", sawStandIns, "most block displays at once: " + maxDisplays);
                cmd(mc, "say vein done");
            })
            .waitTicks(60)
            .run(mc -> SelfTest.record("mp displays cleaned up", standIns(mc) == 0, standIns(mc) + " block displays left"))
            .waitTicks(300);
        return s;
    }
}
