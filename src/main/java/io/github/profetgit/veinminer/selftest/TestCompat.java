package io.github.profetgit.veinminer.selftest;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
//? if <1.21.2 {
/*import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.GameRules;
*///?}

/**
 * What the test driver does differently across Minecraft versions, so a port (tools/Backport, the port skill) touches
 * this file only. Branches from mods/SnappyBlocks' TestCompat (verified on 26.3 and 1.21.1).
 */
final class TestCompat {
    private TestCompat() {}

    static void createWorld(Minecraft mc, String name) {
//? if >=1.21.2 {
        mc.createWorldOpenFlows().createFreshLevel(name,
            new LevelSettings(name, GameType.SURVIVAL, new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT),
            new WorldOptions(42L, false, false), WorldPresets::createTestWorldDimensions, mc.gui.screen());
//?} else {
/*        mc.createWorldOpenFlows().createFreshLevel(name,
            new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
            new WorldOptions(42L, false, false),
            access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.gui.screen());
*///?}
    }

    static void screenshot(RenderTarget target, Consumer<NativeImage> then) {
//? if >=1.21.2 {
        Screenshot.takeScreenshot(target, then);
//?} else {
/*        then.accept(Screenshot.takeScreenshot(target));
*///?}
    }

    static String versionName() {
//? if >=1.21.6 {
        return net.minecraft.SharedConstants.getCurrentVersion().name();
//?} else {
/*        return net.minecraft.SharedConstants.getCurrentVersion().getName();
*///?}
    }
}
