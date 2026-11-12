package io.github.profetgit.veinminer.client.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import org.jspecify.annotations.Nullable;

/** What the client knows about blocks and block tags, for the custom block list: names, icons and lookups of {@code custom_blocks} entries. */
final class BlockIndex {
    /** A block the player can search for. */
    record Entry(String id, String name, String lower, Block block) {}

    /** A block tag the client knows (only inside a world). */
    record Tag(String id, String name, int size, @Nullable Block first) {}

    private BlockIndex() {}

    static List<Entry> blocks() {
        List<Entry> out = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block.defaultBlockState().isAir() || block instanceof LiquidBlock || block instanceof BubbleColumnBlock) continue;
            // no item form (portals, fire, moving pistons) or unbreakable (bedrock, barriers): never mined, so never veined
            if (block.asItem() == net.minecraft.world.item.Items.AIR
                || block.defaultBlockState().getDestroySpeed(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO) < 0) continue;
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            String name = block.getName().getString();
            out.add(new Entry(id, name, name.toLowerCase(Locale.ROOT), block));
        }
        out.sort(Comparator.comparing(Entry::lower).thenComparing(Entry::id));
        return out;
    }

    static List<Tag> tags() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return List.of();
        try {
            List<Tag> out = new ArrayList<>();
            Compat.blockTags().forEach(named -> {
                String id = named.key().location().toString();
                Block first = named.stream().map(h -> h.value()).findFirst().orElse(null);
                out.add(new Tag(id, tagName(id), named.size(), first));
            });
            out.sort(Comparator.comparing(Tag::id));
            return out;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /** The block for {@code namespace:path}, or null when the id is bad or no such block is installed. */
    static @Nullable Block block(String id) {
        Identifier key = Identifier.tryParse(id);
        if (key == null) return null;
        Optional<Block> found = BuiltInRegistries.BLOCK.getOptional(key);
        return found.orElse(null);
    }

    static boolean holdsItems(Block block) {
        return block instanceof EntityBlock;
    }

    static String idOf(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    static ItemStack icon(@Nullable Block block) {
        return block == null ? ItemStack.EMPTY : block.asItem().getDefaultInstance();
    }

    static String tagName(String id) {
        int colon = id.indexOf(':');
        return VeinminerScreen.pretty((colon < 0 ? id : id.substring(colon + 1)).replace('/', '_'));
    }

    /** What a player reads for a {@code custom_blocks} entry: "Netherrack", "Logs (tag)", "minecraft:nope (not found)". */
    static String label(String entry) {
        if (entry.startsWith("#")) {
            String id = entry.substring(1);
            List<Tag> known = tags();
            if (!known.isEmpty() && known.stream().noneMatch(t -> t.id().equals(id))) return entry + " (not found)";
            return tagName(id) + " (tag)";
        }
        Block block = block(entry);
        return block == null ? entry + " (not found)" : block.getName().getString();
    }

    static ItemStack icon(String entry) {
        if (entry.startsWith("#")) {
            String id = entry.substring(1);
            for (Tag t : tags()) if (t.id().equals(id)) return icon(t.first());
            return ItemStack.EMPTY;
        }
        return icon(block(entry));
    }
}
