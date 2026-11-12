package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.platform.Platform;
import java.util.List;

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
        if (only.equals("all") || List.of(only.split(",")).contains("chain")) {
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
        if (only.contains("real")) s.server(RealMods::run).waitTicks(20);
        if (only.contains("demo")) s.server(Demo::build).waitTicks(100);
        if (only.contains("perf")) {
            s.server(Perf::warm).waitTicks(20);
            for (int i = 0; i < 3; i++) {
                s.server(Perf::startRun).until("the perf chain to end", 800, mc -> Perf.done()).waitTicks(10).server(Perf::endRun).waitTicks(10);
            }
            s.server(Perf::report);
        }
        if (only.contains("chainview")) {
            for (String subject : ChainView.SUBJECTS) {
                if (only.equals("chainview") || only.contains(subject)) chainView(s, subject);
            }
        }
        if (only.equals("all") || only.contains("groups")) {
            s.server(GroupsScene::run)
                .server(GroupsScene::startReload)
                .until("the data reload", 1200, mc -> GroupsScene.reloaded())
                .waitTicks(5)
                .server(GroupsScene::afterReload);
        }
        if (only.equals("all") || only.contains("blockentity")) s.server(BlockEntityScene::run);
        if (only.equals("all") || only.contains("ground")) {
            s.server(GroundScene::instant);
            for (String name : GroundScene.LIVE) {
                s.server(server -> GroundScene.start(server, name))
                    .until("the ground case " + name, 900, mc -> GroundScene.poll(mc, name))
                    .server(server -> GroundScene.finish(server, name));
            }
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

    private static void chainView(Script s, String subject) {
        s.server(server -> ChainView.build(server, subject))
            .waitTicks(80)
            .run(ChainView::aim)
            .waitTicks(20)
            .capture(subject, 1500)
            .waitTicks(6)
            .server(ChainView::strike)
            .until("the " + subject + " chain to end", 800, mc -> ChainView.done())
            .waitTicks(30)
            .stopCapture()
            .until(subject + " frames written", 600, mc -> !SelfTest.capturing())
            .server(server -> ChainView.finish(server, subject))
            .waitTicks(20);
    }

    private static void smoke(Script s) {
        s.check("mod loaded", mc -> Platform.get().isModLoaded("veinminer") ? null : "veinminer not loaded")
            .check("player alive", mc -> mc.player.isAlive() ? null : "player not alive")
            .capture("smoke", 1)
            .until("smoke frame written", 200, mc -> !SelfTest.capturing())
            .waitTicks(10);
    }
}
