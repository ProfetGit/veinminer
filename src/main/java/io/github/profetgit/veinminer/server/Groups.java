package io.github.profetgit.veinminer.server;

import io.github.profetgit.veinminer.Veinminer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import org.jspecify.annotations.Nullable;

/**
 * PRD 5.2: which vein group a block belongs to. The whole block-to-group table is built from the loaded tags on the
 * first lookup after a tag reload ({@link #invalidate}), so a lookup is one hash get, and a block that is no ore costs
 * nothing more. Server thread only.
 */
public final class Groups {
    private static final List<String> VANILLA = List.of("coal", "iron", "copper", "gold", "redstone", "lapis", "diamond", "emerald");
    private static final String[] PREFIXES = {"deepslate_", "nether_", "end_", "blackstone_", "netherrack_"};

//? if >=1.21 {
    private static final List<TagKey<Block>> COMMON = List.of(commonTag("c", "ores"));
//?} else {
/*    // 1.20.1: Forge tags ore blocks forge:ores and forge:ores/<name>, Fabric c:ores and c:<name>_ores
    private static final List<TagKey<Block>> COMMON = List.of(commonTag("c", "ores"), commonTag("forge", "ores"));
*///?}

    private static volatile Map<Block, String> table;
    private static Map<String, Integer> sizes = Map.of();
    private static Set<String> custom = Set.of();
    private static List<String> unknown = List.of();
    private static int oresSize;
    private static boolean ignoreCommon;

    private Groups() {}

    public static void invalidate() {
        table = null;
    }

    /** Tests: pretend no {@code c:} tags exist, so only the vanilla rule (3) resolves blocks. */
    public static void ignoreCommon(boolean on) {
        ignoreCommon = on;
        invalidate();
    }

    public static @Nullable String of(Block block) {
        Map<Block, String> t = table;
        if (t == null) t = build();
        return t.get(block);
    }

    /** Blocks in {@code #c:ores} (flattened), as of the last build. */
    public static int oresTagSize() {
        if (table == null) build();
        return oresSize;
    }

    /** Whether {@code group} is one of the player's own {@code custom_blocks} entries (the name is the entry). */
    public static boolean isCustom(String group) {
        if (table == null) build();
        return custom.contains(group);
    }

    /** The {@code custom_blocks} entries as of the last build that no installed block or tag matched (skipped, never removed from the file). */
    public static List<String> unknown() {
        if (table == null) build();
        return unknown;
    }

    /** {@code entry} as stored in the file ({@code minecraft:dirt}, {@code #minecraft:logs}), or null when it is no block id or tag id. */
    public static @Nullable String normalize(String entry) {
        String text = entry.trim();
        boolean tag = text.startsWith("#");
        Identifier id = Identifier.tryParse(tag ? text.substring(1) : text);
        if (id == null) return null;
        return (tag ? "#" : "") + id;
    }

    /** The blocks an entry names right now (air and fluids left out), or null when no installed block or tag has that id. */
    public static @Nullable List<Block> resolve(String entry) {
        String norm = normalize(entry);
        if (norm == null) return null;
        List<Block> out = new ArrayList<>();
        if (norm.startsWith("#")) {
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, Identifier.tryParse(norm.substring(1)));
            var set = BuiltInRegistries.BLOCK.get(tag);
            if (set.isEmpty()) return null;
            for (var holder : set.get()) if (usable(holder.value())) out.add(holder.value());
        } else {
            var block = BuiltInRegistries.BLOCK.getOptional(Identifier.tryParse(norm));
            if (block.isEmpty()) return null;
            if (usable(block.get())) out.add(block.get());
        }
        return out;
    }

    private static boolean usable(Block block) {
        return !(block instanceof LiquidBlock) && !block.defaultBlockState().isAir();
    }

    /** Every block tag id as {@code #ns:path}, for command suggestions. */
    public static List<String> tagNames() {
        List<String> out = new ArrayList<>();
        for (HolderSet.Named<Block> tag : (Iterable<HolderSet.Named<Block>>) BuiltInRegistries.BLOCK.getTags()::iterator) out.add("#" + tag.key().location());
        return out;
    }

    /** The name of {@code group} as a player reads it: a custom block's own name, a readable tag name, else the group name. */
    public static Object label(String group) {
        if (!isCustom(group)) return group;
        if (!group.startsWith("#")) {
            Identifier id = Identifier.tryParse(group);
            if (id != null) {
                var block = BuiltInRegistries.BLOCK.getOptional(id);
                if (block.isPresent()) return block.get().getName();
            }
        }
        return readable(group);
    }

    /** {@code minecraft:polished_andesite} is "Polished Andesite", {@code #c:ores/iron} is "Ores Iron". */
    public static String readable(String entry) {
        String path = entry.startsWith("#") ? entry.substring(1) : entry;
        int colon = path.indexOf(':');
        if (colon >= 0) path = path.substring(colon + 1);
        StringBuilder out = new StringBuilder();
        for (String word : path.replace('/', ' ').replace('_', ' ').split(" ")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.length() == 0 ? entry : out.toString();
    }

    /** Group name to block count, sorted by name. */
    public static Map<String, Integer> sizes() {
        if (table == null) build();
        return sizes;
    }

    /** Rule 4: a block in {@code #c:ores} with no subtag is its own group. */
    public static String derive(String namespace, String path) {
        for (String p : PREFIXES) {
            if (path.startsWith(p)) {
                path = path.substring(p.length());
                break;
            }
        }
        if (path.endsWith("_ore") && path.length() > 4) path = path.substring(0, path.length() - 4);
        return namespace + ":" + path;
    }

    private static Map<Block, String> build() {
        Map<Block, String> t = new HashMap<>();
        int ores = 0;
        Set<String> names = new LinkedHashSet<>();
        List<String> bad = new ArrayList<>();
        for (String entry : Config.get().customBlocks()) {
            List<Block> blocks = resolve(entry);
            if (blocks == null) {
                bad.add(entry);
                continue;
            }
            String name = normalize(entry);
            names.add(name);
            for (Block b : blocks) t.putIfAbsent(b, name);
        }
        if (!bad.isEmpty()) Veinminer.LOG.warn("Veinminer: custom_blocks entries skipped, no such block or tag is installed: {}", bad);
        for (Map.Entry<String, GroupData.Group> e : GroupData.groups().entrySet()) {
            for (TagKey<Block> tag : e.getValue().tags()) {
                for (var holder : BuiltInRegistries.BLOCK.get(tag).map(s -> (Iterable<net.minecraft.core.Holder<Block>>) s).orElse(List.of())) {
                    t.putIfAbsent(holder.value(), e.getKey());
                }
            }
        }
        if (!ignoreCommon) {
            Map<String, HolderSet.Named<Block>> subtags = new TreeMap<>();
            for (HolderSet.Named<Block> tag : (Iterable<HolderSet.Named<Block>>) BuiltInRegistries.BLOCK.getTags()::iterator) {
                Identifier id = tag.key().location();
//? if >=1.21 {
                if (id.getNamespace().equals("c") && id.getPath().startsWith("ores/") && id.getPath().length() > 5) {
                    subtags.put(id.getPath().substring(5), tag);
                }
//?} else {
/*                if ((id.getNamespace().equals("c") || id.getNamespace().equals("forge")) && id.getPath().startsWith("ores/") && id.getPath().length() > 5) {
                    subtags.put(id.getPath().substring(5), tag);
                } else if (id.getNamespace().equals("c") && id.getPath().endsWith("_ores") && id.getPath().length() > 5) {
                    subtags.putIfAbsent(id.getPath().substring(0, id.getPath().length() - 5), tag);
                }
*///?}
            }
            for (Map.Entry<String, HolderSet.Named<Block>> e : subtags.entrySet()) {
                for (var holder : e.getValue()) t.putIfAbsent(holder.value(), e.getKey());
            }
        }
        for (String name : VANILLA) {
            for (var holder : BuiltInRegistries.BLOCK.get(vanillaTag(name + "_ores")).map(s -> (Iterable<net.minecraft.core.Holder<Block>>) s).orElse(List.of())) {
                t.putIfAbsent(holder.value(), name);
            }
        }
        t.putIfAbsent(Blocks.NETHER_QUARTZ_ORE, "quartz");
        t.putIfAbsent(Blocks.ANCIENT_DEBRIS, "netherite_scrap");
        if (!ignoreCommon) {
            for (TagKey<Block> commonTag : COMMON) {
                var common = BuiltInRegistries.BLOCK.get(commonTag);
                if (common.isPresent()) {
                    for (var holder : common.get()) {
                        ores++;
                        Block b = holder.value();
                        if (!t.containsKey(b)) {
                            Identifier id = BuiltInRegistries.BLOCK.getKey(b);
                            t.put(b, derive(id.getNamespace(), id.getPath()));
                        }
                    }
                }
            }
        }
        for (var holder : BuiltInRegistries.BLOCK.get(GroupData.NEVER).map(s -> (Iterable<net.minecraft.core.Holder<Block>>) s).orElse(List.of())) {
            t.remove(holder.value());
        }
        Map<String, Integer> counts = new TreeMap<>();
        for (String g : t.values()) counts.merge(g, 1, Integer::sum);
        custom = Set.copyOf(names);
        unknown = List.copyOf(bad);
        sizes = counts;
        table = t;
        oresSize = ores;
        return t;
    }

    private static TagKey<Block> commonTag(String namespace, String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(namespace, path));
    }

    private static TagKey<Block> vanillaTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(path));
    }

    /** The line logged once at server start: the stop/go evidence for {@code #c:ores} existing at runtime. */
    public static String report() {
        Map<String, Integer> s = sizes();
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> e : s.entrySet()) parts.add(e.getKey() + "=" + e.getValue());
        return "#c:ores holds " + oresTagSize() + " blocks; " + s.size() + " groups (" + String.join(", ", parts) + ")";
    }

    static void logReport() {
        Veinminer.LOG.info("Veinminer: {}", report());
    }
}
