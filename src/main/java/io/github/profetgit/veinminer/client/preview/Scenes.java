package io.github.profetgit.veinminer.client.preview;

import io.github.profetgit.profetsettings.Ctx;
import io.github.profetgit.profetsettings.Gfx;
import io.github.profetgit.veinminer.core.Arc;
import io.github.profetgit.veinminer.core.Delays;
import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.Groups;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// One scene per option of the Veinminer screen. Each is a vein breaking (VeinScene) with what its option changes.
final class Scenes {
    private Scenes() {}

    static final class Chain extends VeinScene {
        private final Subject subject = Subject.iron();

        @Override
        Subject subject() {
            return subject;
        }

        @Override
        String off(Config.Values c) {
            return c.animation() ? null : t("off.chain", "Chain is off: all at once");
        }
    }

    // veins start whether or not the player sneaks when the option is off; with it on the key is shown held
    static final class Sneak extends VeinScene {
        private final Subject subject = Subject.lapis();

        @Override
        Subject subject() {
            return subject;
        }

        @Override
        int signature(Config.Values c) {
            return c.requireSneak() ? 1 : 0;
        }

        @Override
        String off(Config.Values c) {
            return c.requireSneak() ? null : t("off.sneak", "Sneak is off: any block veins");
        }

        @Override
        void overlay(Gfx g, Ctx c, int x, int y, int w, int h, float time, double tau, Run run) {
            if (!Previews.config().requireSneak()) return;
            float from = HIT1 - RAISE - 150, to = BREAK + 700;
            if (time < from || time >= to) return;
            float a = g.alpha;
            g.alpha *= Math.min(1, Math.min((time - from) / 120, (to - time) / 200));
            g.binding(Minecraft.getInstance().options.keyShift, x + w / 2f, y + 120, c.sz.fs, time >= HIT1 - 100);
            g.alpha = a;
        }
    }

    // the chain stops after Most blocks, the struck one included: what is left stays
    static final class Size extends VeinScene {
        private final Subject subject = Subject.copper();

        @Override
        Subject subject() {
            return subject;
        }

        @Override
        int[] area() {
            return new int[] {2, 128};
        }

        @Override
        Run.Opts opts(Config.Values c) {
            return new Run.Opts(true, c.animation(), c.maxBlocks(), c.diagonal(), c.drops() == 1, c.durability());
        }
    }

    static final class Diagonal extends VeinScene {
        private final Subject subject = Subject.gold();

        @Override
        Subject subject() {
            return subject;
        }

        @Override
        String off(Config.Values c) {
            return c.diagonal() ? null : t("off.diagonal", "Corners are off: faces only");
        }
    }

    // the pickaxe in the player's own hotbar with the game's durability bar: one point per mined block, or only the struck one
    static final class Wear extends VeinScene {
        private static final int MAX = 32, START = 14;
        private static final float EASE = 450;
        private final Subject subject = Subject.coal();
        private ItemStack stack;

        @Override
        Subject subject() {
            return subject;
        }

        @Override
        int[] area() {
            return new int[] {0, 72};
        }

        @Override
        int lootBottom() {
            return 36;
        }

        @Override
        Run.Opts opts(Config.Values c) {
            return new Run.Opts(true, c.animation(), Math.max(c.maxBlocks(), 12), c.diagonal(), false, c.durability());
        }

        @Override
        String off(Config.Values c) {
            return c.durability() ? null : t("off.wear", "Wear is off: one point total");
        }

        private int cost(Run run, double tau) {
            int cost = 0;
            for (int i : run.order) if (tau >= run.delay[i] && tau >= 0 && (i == 0 || run.opts.durability())) cost++;
            return cost;
        }

        @Override
        void overlay(Gfx g, Ctx c, int x, int y, int w, int h, float time, double tau, Run run) {
            int total = cost(run, 1000);
            double damage = START + cost(run, tau);
            if (time < EASE) {
                double u = time / EASE;
                u = u * u * (3 - 2 * u);
                damage = START + total + (START - START - total) * u;
            }
            int bx = x + (w - 182) / 2, by = y + 111;
            Hotbar.draw(g, bx, by, null);
            int d = (int) Math.round(damage);
            if (itemsReady()) {
                if (stack == null) stack = new ItemStack(Items.GOLDEN_PICKAXE);
                stack.setDamageValue(d);
                g.item(stack, bx + 3, by + 3);
                g.itemDecorations(stack, bx + 3, by + 3);
            } else {
                g.blit(itemTex("golden_pickaxe"), bx + 3, by + 3, 0, 0, 16, 16, 16, 16);
                int bar = Mth.clamp(Math.round(13f - d * 13f / MAX), 0, 13);
                g.rect(bx + 5, by + 16, 13, 2, 0xFF000000);
                g.rect(bx + 5, by + 16, bar, 1, 0xFF000000 | Mth.hsvToRgb(Math.max(0f, (MAX - d) / (float) MAX) / 3f, 1f, 1f));
            }
        }
    }

    // where the loot lands: in front of the vein, or into the player's hotbar at once
    static final class Loot extends VeinScene {
        private static final int SLOT = 4;
        private final Subject subject = Subject.redstone();

        @Override
        Subject subject() {
            return subject;
        }

        @Override
        int[] area() {
            return new int[] {4, 68};
        }

        @Override
        int lootBottom() {
            return 36;
        }

        @Override
        float[] slotTarget(int x, int y, int w) {
            return new float[] {x + (w - 182) / 2f + 3 + 20 * SLOT + 8, y + 111 + 3 + 8};
        }

        @Override
        String off(Config.Values c) {
            return c.animation() || c.drops() == 1 ? null : t("off.loot", "Chain is off: loot stays put");
        }

        @Override
        void overlay(Gfx g, Ctx c, int x, int y, int w, int h, float time, double tau, Run run) {
            int bx = x + (w - 182) / 2, by = y + 111;
            Hotbar.draw(g, bx, by, subject.tool().texture());
            if (!run.toFeet() && !(run.chainMode && run.opts.own())) return;
            double at = tau;
            float alpha = 1;
            if (time < TAIL) {
                at = (time + period - BREAK) / MS;
                alpha = 1 - time / TAIL;
            }
            int count = 0;
            for (Run.Ghost gh : run.ghosts) if (at >= gh.born + Arc.LAUNCH + Arc.LAND + PICKUP) count += subject.count();
            if (run.toFeet() && at >= PICKUP) count = run.pops.size() * subject.count();
            if (count == 0 && time >= TAIL) return;
            if (count == 0) return;
            float was = g.alpha;
            g.alpha *= alpha;
            int ix = bx + 3 + 20 * SLOT, iy = by + 3;
            g.blit(subject.item(), ix, iy, 0, 0, 16, 16, 16, 16);
            String n = Integer.toString(count);
            g.vanilla(n, ix + 17 - g.vanillaWidth(n), iy + 9, 0xFFFFFFFF, true);
            g.alpha = was;
        }
    }

    // vanilla's action-bar text over the player's own hotbar, with the mod's message once the vein is done
    static final class Messages extends VeinScene {
        private final Subject subject = Subject.diamondSmall();

        @Override
        Subject subject() {
            return subject;
        }

        @Override
        int[] area() {
            return new int[] {4, 66};
        }

        @Override
        int lootBottom() {
            return 36;
        }

        @Override
        Run.Opts opts(Config.Values c) {
            return new Run.Opts(true, c.animation(), Math.max(c.maxBlocks(), 12), c.diagonal(), false, c.durability());
        }

        @Override
        int signature(Config.Values c) {
            return c.feedback() ? 1 : 0;
        }

        @Override
        String off(Config.Values c) {
            return c.feedback() ? null : t("off.messages", "Messages are off: silent");
        }

        private int sent(Run run) {
            return run.chainMode ? run.maxDelay + Delays.TAIL : 1;
        }

        @Override
        float hold(Run run) {
            float end = BREAK + (sent(run) + 60) * MS;
            return Math.max(0, end - (BREAK + run.lastTick * MS + REST));
        }

        @Override
        void overlay(Gfx g, Ctx c, int x, int y, int w, int h, float time, double tau, Run run) {
            int bar = x + (w - 182) / 2, by = y + 111;
            Hotbar.draw(g, bar, by, subject.tool().texture());
            if (!Previews.config().feedback()) return;
            double left = 60 - (tau - sent(run));
            if (left <= 0 || left > 60) return;
            int n = run.order.size();
            String label = Groups.readable(run.subject.name());
            String text = Component.translatableWithFallback("veinminer.msg.veined", "Veined " + n + " " + label, n, label).getString();
            float a = g.alpha;
            g.alpha *= (float) Math.min(1, left / 20);
            int tw = g.vanillaWidth(text), tx = x + (w - tw) / 2, ty = by - 32;
            g.rect(tx - 2, ty - 2, tw + 4, 12, 0x66000000);
            g.vanilla(text, tx, ty, 0xFFFFFFFF, true);
            g.alpha = a;
        }
    }

    // a group's own vein: with the group off only the struck block breaks
    static final class Group extends VeinScene {
        private final String group;
        private final Subject subject;

        Group(String group) {
            this.group = group;
            this.subject = Subject.group(group);
        }

        @Override
        Subject subject() {
            return subject;
        }

        @Override
        Run.Opts opts(Config.Values c) {
            return new Run.Opts(c.groupOn(group), c.animation(), Math.max(c.maxBlocks(), 12), c.diagonal(), c.drops() == 1, c.durability());
        }

        @Override
        String off(Config.Values c) {
            if (c.groupOn(group)) return null;
            return t("off.group", "%s is off: one block", Groups.readable(group));
        }
    }

    // ---- shared bits ----

    // item stacks can only be made once a world has loaded their components
    static boolean itemsReady() {
        //? if >=26.1 {
        return Items.APPLE.builtInRegistryHolder().areComponentsBound();
        //?} else {
        /*return true;
        *///?}
    }

    // vanilla's hotbar, the pickaxe selected, with a couple of other items from the player's own resources
    static final class Hotbar {
        //? if >=1.20.2 {
        private static final Identifier BAR = Identifier.tryParse("hud/hotbar"), SELECT = Identifier.tryParse("hud/hotbar_selection");
        //?} else {
        /*private static final Identifier WIDGETS = Identifier.tryParse("textures/gui/widgets.png");
        *///?}

        private Hotbar() {}

        // {@code tool}: draw the pickaxe in the first slot as a flat icon (the wear scene draws its own, with the bar)
        static void draw(Gfx g, int x, int y, Identifier tool) {
            //? if >=1.20.2 {
            g.sprite(BAR, x, y, 182, 22);
            g.sprite(SELECT, x - 1, y - 1, 24, 23);
            //?} else {
            /*g.blit(WIDGETS, x, y, 0, 0, 182, 22, 256, 256);
            g.blit(WIDGETS, x - 1, y - 1, 0, 22, 24, 24, 256, 256);
            *///?}
            if (tool != null) g.blit(tool, x + 3, y + 3, 0, 0, 16, 16, 16, 16);
            g.blit(VeinScene.itemTex("bread"), x + 23, y + 3, 0, 0, 16, 16, 16, 16);
            g.blit(VeinScene.itemTex("apple"), x + 43, y + 3, 0, 0, 16, 16, 16, 16);
        }
    }
}
