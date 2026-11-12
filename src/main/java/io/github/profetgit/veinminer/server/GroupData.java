package io.github.profetgit.veinminer.server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.profetgit.veinminer.Veinminer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * PRD 5.3: the loaded data groups and {@code #veinminer:never}. The group name is the file's path; files of the same
 * name (from any namespace) add their tags together, and a name that is a built-in group extends it. The tags are only
 * remembered here and resolved when {@link Groups} builds its table, so the reload order against the tag reload does not
 * matter.
 */
public final class GroupData {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Veinminer.ID, "groups");
    public static final TagKey<Block> NEVER = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Veinminer.ID, "never"));

    public record Group(List<TagKey<Block>> tags, @Nullable Integer maxBlocks, GroupDef.Tool tool, boolean defaultOn) {}

    private static volatile Map<String, Group> groups = Map.of();
    private static volatile Map<Identifier, GroupDef> loaded = Map.of();
    private static volatile int applied;

    private GroupData() {}

    /** The server data reload listener for {@code data/<ns>/veinminer/group/*.json}; each loader's entry registers it as {@link #ID}. */
    public static PreparableReloadListener listener() {
        return new Listener();
    }

    /** The files behind the current data groups. */
    public static Map<Identifier, GroupDef> loaded() {
        return loaded;
    }

    /** How many times the groups were replaced (tests wait on it after {@code /reload}). */
    public static int applied() {
        return applied;
    }

    /** Data groups by name, sorted. */
    public static Map<String, Group> groups() {
        return groups;
    }

    /**
     * The block cap of a vein of {@code group}: the group file's {@code max_blocks} when it has one (it may be above the
     * config's cap: the pack author chose it), else the config's.
     */
    public static int cap(String group, int configCap) {
        Group g = groups.get(group);
        return g != null && g.maxBlocks() != null ? g.maxBlocks() : configCap;
    }

    /** What starts a vein of {@code group}: {@code CORRECT} when any file of that name says so. */
    public static GroupDef.Tool tool(String group) {
        Group g = groups.get(group);
        return g != null ? g.tool() : GroupDef.Tool.PICKAXE;
    }

    /** Whether {@code group} is on while the config has no entry for it: off when any file of that name says {@code "default": false}; built-in ores are on. */
    public static boolean defaultOn(String group) {
        Group g = groups.get(group);
        return g == null || g.defaultOn();
    }

    /** Replaces the data groups with those parsed from {@code defs} (keys are file ids) and rebuilds the block table. */
    public static void apply(Map<Identifier, GroupDef> defs) {
        List<Identifier> ids = new ArrayList<>(defs.keySet());
        ids.sort(null);
        Map<String, List<TagKey<Block>>> tags = new TreeMap<>();
        Map<String, Integer> caps = new TreeMap<>();
        Map<String, GroupDef.Tool> tools = new TreeMap<>();
        Map<String, Boolean> defaults = new TreeMap<>();
        for (Identifier id : ids) {
            GroupDef d = defs.get(id);
            tags.computeIfAbsent(id.getPath(), k -> new ArrayList<>()).add(d.blocks());
            if (d.maxBlocks() != null) caps.merge(id.getPath(), d.maxBlocks(), Math::max);
            tools.merge(id.getPath(), d.tool(), (a, b) -> a == GroupDef.Tool.CORRECT || b == GroupDef.Tool.CORRECT ? GroupDef.Tool.CORRECT : GroupDef.Tool.PICKAXE);
            defaults.merge(id.getPath(), d.defaultOn(), (a, b) -> a && b);
        }
        Map<String, Group> out = new TreeMap<>();
        tags.forEach((name, list) -> out.put(name, new Group(List.copyOf(list), caps.get(name), tools.get(name), defaults.get(name))));
        groups = Map.copyOf(out);
        loaded = Map.copyOf(defs);
        applied++;
        Groups.invalidate();
        Veinminer.LOG.info("Veinminer: data groups {}", ids);
    }

//? if >=1.21.2 {
    private static final class Listener extends SimpleJsonResourceReloadListener<GroupDef> {
        Listener() {
            super(GroupDef.CODEC, FileToIdConverter.json("veinminer/group"));
        }

        @Override
        protected void apply(Map<Identifier, GroupDef> defs, ResourceManager manager, ProfilerFiller profiler) {
            GroupData.apply(defs);
        }
    }
//?} else {
/*    private static final class Listener extends SimpleJsonResourceReloadListener {
        Listener() {
            super(new Gson(), "veinminer/group");
        }

        @Override
        protected void apply(Map<Identifier, JsonElement> json, ResourceManager manager, ProfilerFiller profiler) {
            Map<Identifier, GroupDef> defs = new HashMap<>();
            json.forEach((id, element) -> GroupDef.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Veinminer.LOG.error("Veinminer: group {} ignored: {}", id, error)).ifPresent(d -> defs.put(id, d)));
            GroupData.apply(defs);
        }
    }
*///?}
}
