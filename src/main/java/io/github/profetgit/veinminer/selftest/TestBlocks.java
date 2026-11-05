package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.Veinminer;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * PRD 12 {@code modded}: ore-like blocks that drop themselves (in code, not a loot table), registered only for a self-test run that names the scene
 * (a normal game never sees them). Tagged by the mod's own data: tin and the block-entity ore in {@code c:ores/tin},
 * zinc in {@code c:ores/zinc}, odd only in {@code c:ores}.
 */
public final class TestBlocks {
    public static Block tin, zinc, odd, be;
    static BlockEntityType<TestBe> beType;

    private TestBlocks() {}

    public static boolean wanted() {
        if (System.getProperty("veinminer.selftest") == null) return false;
        String scene = System.getProperty("veinminer.selftest.scene", "all");
        return scene.equals("all") || scene.contains("modded") || scene.contains("chainview");
    }

    public static void register() {
        tin = ore("test_ore_tin", false);
        zinc = ore("test_ore_zinc", false);
        odd = ore("test_ore_odd", false);
        be = ore("test_ore_be", true);
        beType = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(Veinminer.ID, "test_ore_be"),
            new BlockEntityType<>(TestBe::new, Set.of(be)));
    }

    private static Block ore(String name, boolean entity) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Veinminer.ID, name));
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(3f).requiresCorrectToolForDrops().setId(key);
        Block block = Registry.register(BuiltInRegistries.BLOCK, key, entity ? new EntityOre(props) : new Ore(props));
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, key.identifier());
        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
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
