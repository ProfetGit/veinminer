package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.Veinminer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * PRD 12 {@code modded} and M3 {@code groups}/{@code blockentity}: ore-like blocks that drop themselves (in code, not a loot table), registered only for a self-test run that names the scene
 * (a normal game never sees them). Tagged by the mod's own data: tin and the block-entity ore in {@code c:ores/tin},
 * zinc in {@code c:ores/zinc}, odd only in {@code c:ores}.
 */
public final class TestBlocks {
    public static Block tin, zinc, odd, be, ext, ga, win, cap, never, custom;
    /** Times {@link CustomOre#playerDestroy} ran, and the bonus item it drops on top of the ore itself. */
    public static int customBreaks;
    public static final Item CUSTOM_BONUS = Items.AMETHYST_SHARD;
    static BlockEntityType<TestBe> beType;

    private TestBlocks() {}

    public static boolean wanted() {
        if (System.getProperty("veinminer.selftest") == null) return false;
        String scene = System.getProperty("veinminer.selftest.scene", "all");
        return scene.equals("all") || scene.contains("modded") || scene.contains("groups") || scene.contains("blockentity") || scene.contains("chainview");
    }

    public static void register() {
        registerBlocks((key, block) -> Registry.register(BuiltInRegistries.BLOCK, key, block));
        registerItems((key, item) -> Registry.register(BuiltInRegistries.ITEM, key, item));
        registerEntityTypes((key, type) -> Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, type));
    }

    /** The three registrations apart, so a loader that only accepts a registry's entries during its own RegisterEvent (Forge 47) can feed each one. */
    public static void registerBlocks(BiConsumer<ResourceKey<Block>, Block> sink) {
        tin = ore(sink, "test_ore_tin", false);
        zinc = ore(sink, "test_ore_zinc", false);
        odd = ore(sink, "test_ore_odd", false);
        be = ore(sink, "test_ore_be", true);
        ext = ore(sink, "test_ore_ext", false);
        ga = ore(sink, "test_ore_ga", false);
        win = ore(sink, "test_ore_win", false);
        cap = ore(sink, "test_ore_cap", false);
        never = ore(sink, "test_ore_never", false);
        custom = ore(sink, "test_ore_custom", false);
    }

    public static void registerItems(BiConsumer<ResourceKey<Item>, Item> sink) {
        for (Map.Entry<ResourceKey<Block>, Block> e : BLOCKS.entrySet()) {
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, e.getKey().identifier());
            sink.accept(itemKey, new BlockItem(e.getValue(), new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        }
    }

    public static void registerEntityTypes(BiConsumer<ResourceKey<BlockEntityType<?>>, BlockEntityType<?>> sink) {
        beType = new BlockEntityType<>(TestBe::new, Set.of(be));
        sink.accept(ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(Veinminer.ID, "test_ore_be")), beType);
    }

    private static final Map<ResourceKey<Block>, Block> BLOCKS = new LinkedHashMap<>();

    private static Block ore(BiConsumer<ResourceKey<Block>, Block> sink, String name, boolean entity) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Veinminer.ID, name));
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(3f).requiresCorrectToolForDrops().setId(key);
        Block block = entity ? new EntityOre(props) : name.equals("test_ore_custom") ? new CustomOre(props) : new Ore(props);
        sink.accept(key, block);
        BLOCKS.put(key, block);
        return block;
    }

    /** Drops itself, with no loot table file: a table for a block that is not registered would break the world load. */
    private static class Ore extends Block {
        Ore(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return List.of(new ItemStack(this));
        }
    }

    /** A block with its own break behaviour: {@code playerDestroy} counts itself and drops a bonus item besides the ore. */
    private static final class CustomOre extends Ore {
        CustomOre(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
//? if >=26.3 {
        public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack destroyedWith) {
//?} else {
/*        public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack destroyedWith) {
*///?}
            customBreaks++;
            popResource(level, pos, new ItemStack(CUSTOM_BONUS));
            super.playerDestroy(level, player, pos, state, blockEntity, destroyedWith);
        }
    }

    private static final class EntityOre extends Ore implements EntityBlock {
        EntityOre(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TestBe(pos, state);
        }
    }

    static final class TestBe extends BlockEntity {
        TestBe(BlockPos pos, BlockState state) {
            super(beType, pos, state);
        }
    }
}
