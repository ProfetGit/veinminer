package io.github.profetgit.veinminer.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * One {@code data/<ns>/veinminer/group/<id>.json} (PRD 5.3): {@code {"blocks": "#ns:tag", "max_blocks": 1..1024?}}.
 * {@code xp_toggle} is accepted and ignored: XP is whatever the real break path pays, there is nothing to toggle.
 */
public record GroupDef(TagKey<Block> blocks, @Nullable Integer maxBlocks) {
    public static final int MAX_CAP = 1024;
    public static final Codec<GroupDef> CODEC = ExtraCodecs.JSON.flatXmap(GroupDef::parse, d -> DataResult.error(() -> "group files are read-only"));

    public static DataResult<GroupDef> parse(JsonElement json) {
        if (!(json instanceof JsonObject o)) return DataResult.error(() -> "a group file must be an object");
        if (!(o.get("blocks") instanceof JsonPrimitive p) || !p.isString() || !p.getAsString().startsWith("#")) {
            return DataResult.error(() -> "\"blocks\" must be a block tag such as \"#c:ores/tin\"");
        }
        Identifier tag = Identifier.tryParse(p.getAsString().substring(1));
        if (tag == null) return DataResult.error(() -> "\"blocks\" is not a valid tag id: " + p.getAsString());
        Integer cap = null;
        JsonElement m = o.get("max_blocks");
        if (m != null && !m.isJsonNull()) {
            if (!(m instanceof JsonPrimitive n) || !n.isNumber() || n.getAsDouble() != n.getAsInt() || n.getAsInt() < 1 || n.getAsInt() > MAX_CAP) {
                return DataResult.error(() -> "\"max_blocks\" must be a whole number from 1 to " + MAX_CAP);
            }
            cap = m.getAsInt();
        }
        return DataResult.success(new GroupDef(TagKey.create(Registries.BLOCK, tag), cap));
    }
}
