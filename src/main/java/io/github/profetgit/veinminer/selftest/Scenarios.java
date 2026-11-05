package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.platform.Platform;

final class Scenarios {
    private Scenarios() {}

    static Script build() {
        Script s = new Script();
        s.run(mc -> TestCompat.createWorld(mc, "veinminertest-" + System.currentTimeMillis()))
            .until("world to load", 2400, mc -> mc.player != null && mc.level != null && mc.gui.screen() == null)
            .waitTicks(40)
            .check("world loaded", mc -> mc.player.isAlive() ? null : "player not alive");
        String only = System.getProperty("veinminer.selftest.scene", "all");
        if (only.equals("all") || only.contains("smoke")) smoke(s);
        if (only.equals("all") || only.contains("modded")) s.server(Modded::run);
        if (only.equals("all") || only.contains("fair")) s.server(Fair::run);
        if (only.equals("all") || only.contains("chain")) {
            s.server(ChainScene::manual)
                .server(ChainScene::realStart)
                .run(mc -> {
                    mc.player.setYRot(180f);
                    mc.player.setXRot(8f);
                })
                .capture("chain", 150)
                .until("the real chain to end", 800, mc -> ChainScene.realDone())
                .until("chain frames written", 600, mc -> !SelfTest.capturing())
                .server(ChainScene::realCheck);
        }
        if (M4.want(only, "conflict")) s.server(Gates::conflict);
        if (M4.want(only, "fake")) s.server(Gates::fake);
        if (M4.want(only, "claims")) s.server(Gates::claims);
        if (M4.want(only, "lag")) s.server(Gates::lag);
        if (M4.want(only, "players")) s.server(Persist::players);
        if (M4.want(only, "config_reload")) {
            s.server(Persist::configEdit).waitTicks(60)
                .server(Persist::configCheckEdit).waitTicks(60)
                .server(Persist::configCheckBroken).waitTicks(60)
                .server(Persist::configCheckFixed);
        }
        if (M4.want(only, "hooks")) {
            s.server(Gates::hooksInstall)
                .until("the add-on's loaded function", 800, mc -> Gates.hooksLoaded()).waitTicks(10)
                .server(Gates::hooksCheck).waitTicks(80)
                .server(Gates::hooksGone);
        }
        if (M4.want(only, "migrate")) {
            s.server(Persist::migrateInstall).waitTicks(100)
                .server(Persist::migrateRun).waitTicks(100)
                .server(Persist::migrateAfter);
        }
        if (M4.want(only, "crash")) {
            s.server(Crash::crash)
                .server(Crash::unloadStart)
                .until("the far chain to end", 800, mc -> Crash.unloadDone()).waitTicks(40)
                .server(Crash::unloadCheck);
        }
        return s;
    }

    private static void smoke(Script s) {
        s.check("mod loaded", mc -> Platform.get().isModLoaded("veinminer") ? null : "veinminer not loaded")
            .check("player alive", mc -> mc.player.isAlive() ? null : "player not alive")
            .capture("smoke", 1)
            .until("smoke frame written", 200, mc -> !SelfTest.capturing())
            .waitTicks(10);
    }
}
