package io.github.profetgit.veinminer.client.preview;

import io.github.profetgit.veinminer.server.Groups;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import org.jspecify.annotations.Nullable;

// What a scene shows: a patch of blocks with their looks, what each drops, the player's tool for them, whether they fall
// when nothing holds them up (sand, gravel). Everything is the player's own: block and item textures are read from the
// resources at draw time.
record Subject(String name, Patch patch, Diorama.Look look, Diorama.Look alt, Diorama.Look cube, Identifier item, int count, Tool tool, boolean falls) {
    Diorama.Look lookOf(int cell) {
        return patch.alt[cell] && alt != null ? alt : look;
    }

    // ---- the shapes ----

    static final Patch IRON = Patch.of("..##/.##S/..#.", "..#./.##./....");
    static final Patch LAPIS = Patch.of("..#./###S/.#..", "..../.##./..#.");
    static final Patch COPPER = Patch.of("####S/#####/#####", "#####/#####/#####");
    static final Patch GOLD = Patch.of("...#S/..#..", "./.#.../#....");
    static final Patch COAL = Patch.of(".#../###S/.##.", "..#./.#../....");
    static final Patch REDSTONE = Patch.of("..##/.#dS/..#.", ".d#./.##./....");
    static final Patch SMALL = Patch.of(".#./##S/.#.", "./..#");
    static final Patch WEAR = Patch.of("###/##S/###");
    static final Patch LOOT = Patch.of("##d/#dS/##d");
    static final Patch MOUND = Patch.of("###/###S/##.", "..##/..##");
    static final Patch[] POOL = {IRON, COAL, REDSTONE, LAPIS};

    private static final Map<String, Subject> CACHE = new HashMap<>();

    static Identifier block(String name) {
        return VeinScene.tex(name);
    }

    static Diorama.Look all(String name) {
        return Diorama.Look.all(block(name));
    }

    private static Subject ore(String name, Patch patch, String stone, @Nullable String deep, String item, int count, Tool tool) {
        return new Subject(name, patch, all(stone), deep == null ? null : all(deep), null, VeinScene.itemTex(item), count, tool, false);
    }

    static Subject iron() {
        return ore("iron", IRON, "iron_ore", "deepslate_iron_ore", "raw_iron", 1, Tool.pickaxe());
    }

    static Subject lapis() {
        return ore("lapis", LAPIS, "lapis_ore", "deepslate_lapis_ore", "lapis_lazuli", 6, Tool.pickaxe());
    }

    static Subject copper() {
        return ore("copper", COPPER, "copper_ore", "deepslate_copper_ore", "raw_copper", 3, Tool.pickaxe());
    }

    static Subject gold() {
        return ore("gold", GOLD, "gold_ore", "deepslate_gold_ore", "raw_gold", 1, Tool.iron());
    }

    static Subject coal() {
        return ore("coal", WEAR, "coal_ore", "deepslate_coal_ore", "coal", 1, Tool.golden());
    }

    static Subject redstone() {
        return ore("redstone", LOOT, "deepslate_redstone_ore", "redstone_ore", "redstone", 4, Tool.iron());
    }

    static Subject diamondSmall() {
        return ore("diamond", SMALL, "deepslate_diamond_ore", "diamond_ore", "diamond", 1, Tool.iron());
    }

    // the vein of a group on the Veinminer screen: the group's own block, found by name, else a plain stone block
    static Subject group(String group) {
        return CACHE.computeIfAbsent(group, Subject::build);
    }

    private static Patch shape(String group) {
        return POOL[Math.floorMod(group.hashCode(), POOL.length)];
    }

    private static Subject build(String group) {
        Patch shape = shape(group);
        switch (group) {
            case "coal": return ore(group, shape, "coal_ore", "deepslate_coal_ore", "coal", 1, Tool.pickaxe());
            case "iron": return ore(group, shape, "iron_ore", "deepslate_iron_ore", "raw_iron", 1, Tool.pickaxe());
            case "copper": return ore(group, shape, "copper_ore", "deepslate_copper_ore", "raw_copper", 3, Tool.pickaxe());
            case "gold": return ore(group, shape, "gold_ore", "deepslate_gold_ore", "raw_gold", 1, Tool.iron());
            case "redstone": return ore(group, shape, "redstone_ore", "deepslate_redstone_ore", "redstone", 4, Tool.iron());
            case "lapis": return ore(group, shape, "lapis_ore", "deepslate_lapis_ore", "lapis_lazuli", 6, Tool.pickaxe());
            case "diamond": return ore(group, shape, "diamond_ore", "deepslate_diamond_ore", "diamond", 1, Tool.iron());
            case "emerald": return ore(group, shape, "emerald_ore", "deepslate_emerald_ore", "emerald", 1, Tool.iron());
            case "quartz": return ore(group, shape, "nether_quartz_ore", null, "quartz", 1, Tool.wooden());
            case "netherite_scrap": {
                Diorama.Look debris = new Diorama.Look(block("ancient_debris_top"), block("ancient_debris_side"), block("ancient_debris_side"));
                return new Subject(group, shape, debris, null, debris, null, 1, Tool.diamond(), false);
            }
            case "dirt": return earth(group, MOUND, all("dirt"), null, 1, false);
            case "sand": return earth(group, MOUND, all("sand"), null, 1, true);
            case "gravel": return earth(group, MOUND, all("gravel"), null, 1, true);
            case "clay": return earth(group, MOUND, all("clay"), VeinScene.itemTex("clay_ball"), 4, false);
            default: return other(group, shape);
        }
    }

    private static Subject earth(String name, Patch patch, Diorama.Look look, @Nullable Identifier item, int count, boolean falls) {
        return new Subject(name, patch, look, null, item == null ? look : null, item, count, Tool.shovel(), falls);
    }

    // a block of another mod, or one of the player's own entries: its name gives the block, its textures the look
    private static Subject other(String group, Patch shape) {
        Block block = find(group);
        if (block == null) return new Subject(group, shape, all("stone"), null, all("stone"), null, 1, Tool.wooden(), false);
        Identifier key = BuiltInRegistries.BLOCK.getKey(block);
        Diorama.Look look = lookOf(key);
        var state = block.defaultBlockState();
        boolean shovel = state.is(BlockTags.MINEABLE_WITH_SHOVEL);
        Tool pick = state.is(BlockTags.NEEDS_DIAMOND_TOOL) ? Tool.diamond() : state.is(BlockTags.NEEDS_IRON_TOOL) ? Tool.iron()
            : state.is(BlockTags.NEEDS_STONE_TOOL) ? Tool.pickaxe() : Tool.wooden();
        boolean falls = block instanceof FallingBlock;
        Patch patch = falls || shovel ? MOUND : shape;
        return new Subject(group, patch, look, null, look, null, 1, shovel ? Tool.shovel() : pick, falls);
    }

    private static @Nullable Block find(String group) {
        List<String> tries = new java.util.ArrayList<>();
        if (group.startsWith("#")) {
            tries.add(group);
        } else if (group.indexOf(':') >= 0) {
            tries.add(group);
            tries.add(group + "_ore");
        } else {
            tries.add("#c:ores/" + group);
            tries.add("minecraft:" + group + "_ore");
            tries.add("minecraft:" + group);
        }
        for (String entry : tries) {
            List<Block> blocks = Groups.resolve(entry);
            if (blocks != null && !blocks.isEmpty()) return blocks.get(0);
        }
        return null;
    }

    private static Diorama.Look lookOf(Identifier key) {
        String ns = key.getNamespace(), path = key.getPath();
        Identifier whole = texture(ns, path);
        if (exists(whole)) return Diorama.Look.all(whole);
        Identifier top = texture(ns, path + "_top"), side = texture(ns, path + "_side");
        if (exists(side)) return new Diorama.Look(exists(top) ? top : side, side, side);
        Identifier front = texture(ns, path + "_front");
        if (exists(front)) return Diorama.Look.all(front);
        return all("stone");
    }

    private static Identifier texture(String ns, String path) {
        return Identifier.tryParse(ns + ":textures/block/" + path.toLowerCase(Locale.ROOT) + ".png");
    }

    static boolean exists(Identifier id) {
        return id != null && Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
    }
}
