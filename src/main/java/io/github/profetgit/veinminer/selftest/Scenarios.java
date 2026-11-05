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
        if (only.contains("chainview")) {
            for (String subject : ChainView.SUBJECTS) {
                if (only.equals("chainview") || only.contains(subject)) chainView(s, subject);
            }
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
