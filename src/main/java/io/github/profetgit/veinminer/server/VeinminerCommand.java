package io.github.profetgit.veinminer.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.ArrayList;
import java.util.List;

/** {@code /veinminer} (toggle yourself, saved), {@code status}, and {@code reload}, {@code set}, {@code add}, {@code remove} and {@code tune} (level 2). */
public final class VeinminerCommand {
    private VeinminerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("veinminer")
            .executes(c -> toggle(c.getSource()))
            .then(Commands.literal("status").executes(c -> status(c.getSource())))
            .then(Commands.literal("reload").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
                Config.load();
                c.getSource().sendSuccess(() -> Component.literal(Config.broken() ? "Veinminer config unreadable, keeping the current values" : "Veinminer config reloaded"), true);
                return 1;
            }))
            .then(add())
            .then(remove())
            .then(set())
            .then(tune()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> set() {
        return Commands.literal("set").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.argument("key", StringArgumentType.string())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Config.KEYS, b))
                .then(Commands.argument("value", StringArgumentType.greedyString()).executes(c -> {
                    String key = StringArgumentType.getString(c, "key");
                    String value = StringArgumentType.getString(c, "value");
                    String err = Config.set(key, value);
                    if (err != null) {
                        c.getSource().sendFailure(Component.literal(err));
                        return 0;
                    }
                    c.getSource().sendSuccess(() -> Component.literal("Veinminer: " + key + " = " + value.trim()), true);
                    return 1;
                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> add() {
        return Commands.literal("add").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .executes(c -> addLooked(c.getSource()))
            .then(Commands.argument("entry", StringArgumentType.greedyString())
                .suggests((c, b) -> {
                    List<String> all = new ArrayList<>();
                    for (var id : BuiltInRegistries.BLOCK.keySet()) all.add(id.toString());
                    all.addAll(Groups.tagNames());
                    return SharedSuggestionProvider.suggest(all, b);
                })
                .executes(c -> addEntry(c.getSource(), StringArgumentType.getString(c, "entry"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> remove() {
        return Commands.literal("remove").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.argument("entry", StringArgumentType.greedyString())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Config.get().customBlocks(), b))
                .executes(c -> removeEntry(c.getSource(), StringArgumentType.getString(c, "entry"))));
    }

    private static int addLooked(CommandSourceStack src) {
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("Only a player can add the block they look at; name a block or #tag instead"));
            return 0;
        }
        if (!(player.pick(5.0, 1.0F, false) instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            src.sendFailure(Component.literal("Look at a block within reach, or name a block or #tag"));
            return 0;
        }
        BlockPos pos = hit.getBlockPos();
        Block block = src.getLevel().getBlockState(pos).getBlock();
        return addEntry(src, BuiltInRegistries.BLOCK.getKey(block).toString());
    }

    private static int addEntry(CommandSourceStack src, String entry) {
        String norm = Groups.normalize(entry);
        if (norm == null) {
            src.sendFailure(Component.literal("\"" + entry.trim() + "\" is not a block id or #tag"));
            return 0;
        }
        List<Block> blocks = Groups.resolve(norm);
        if (blocks == null) {
            src.sendFailure(Component.literal("No block or tag " + norm + " is installed"));
            return 0;
        }
        if (blocks.isEmpty()) {
            src.sendFailure(Component.literal(norm + " has no block a vein can mine (air and fluids never do)"));
            return 0;
        }
        List<String> list = new ArrayList<>(Config.get().customBlocks());
        if (list.contains(norm)) {
            src.sendFailure(Component.literal(norm + " is already on your list"));
            return 0;
        }
        list.add(norm);
        String err = Config.save(Config.get().customBlocks(list));
        if (err != null) {
            src.sendFailure(Component.literal(err));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Veinminer: added " + norm + " (" + blocks.size() + (blocks.size() == 1 ? " block)" : " blocks)")), true);
        return 1;
    }

    private static int removeEntry(CommandSourceStack src, String entry) {
        List<String> list = new ArrayList<>(Config.get().customBlocks());
        String found = list.contains(entry.trim()) ? entry.trim() : null;
        if (found == null) {
            String norm = Groups.normalize(entry);
            if (norm != null) {
                for (String e : list) if (norm.equals(Groups.normalize(e))) found = e;
            }
        }
        if (found == null) {
            src.sendFailure(Component.literal(entry.trim() + " is not on the list"));
            return 0;
        }
        list.remove(found);
        String err = Config.save(Config.get().customBlocks(list));
        if (err != null) {
            src.sendFailure(Component.literal(err));
            return 0;
        }
        String removed = found;
        src.sendSuccess(() -> Component.literal("Veinminer: removed " + removed), true);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tune() {
        LiteralArgumentBuilder<CommandSourceStack> tune = Commands.literal("tune").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .executes(c -> {
                c.getSource().sendSuccess(() -> Component.literal("Veinminer tune: " + Tune.describe()), false);
                return 1;
            });
        for (String name : new String[]{"delay_k", "arc", "squash"}) {
            tune.then(Commands.literal(name).then(Commands.argument("value", DoubleArgumentType.doubleArg()).executes(c -> {
                Tune.set(name, DoubleArgumentType.getDouble(c, "value"));
                c.getSource().sendSuccess(() -> Component.literal("Veinminer tune: " + Tune.describe()), true);
                return 1;
            })));
        }
        return tune;
    }

    private static int toggle(CommandSourceStack src) {
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("Only a player can toggle veining"));
            return 0;
        }
        boolean on = Players.toggle(player);
        src.sendSuccess(() -> Component.literal("Veinminer " + (on ? "on" : "off") + " for you"), false);
        return 1;
    }

    private static int status(CommandSourceStack src) {
        Config.Values v = Config.get();
        src.sendSuccess(() -> Component.literal("max_blocks " + v.maxBlocks() + ", require_sneak " + v.requireSneak() + ", drops " + v.drops()
            + ", durability " + v.durability() + ", diagonal " + v.diagonal() + ", animation " + v.animation() + ", visit_cap " + v.visitCap()
            + ", chains running " + Chain.count()), false);
        src.sendSuccess(() -> Component.literal(Groups.report()), false);
        String off = String.join(", ", Groups.sizes().keySet().stream().filter(g -> !v.groupOn(g)).toList());
        src.sendSuccess(() -> Component.literal("groups off: " + (off.isEmpty() ? "none" : off)), false);
        List<String> mine = new ArrayList<>();
        for (String e : v.customBlocks()) mine.add(Groups.resolve(e) == null ? e + " (not installed, skipped)" : e);
        src.sendSuccess(() -> Component.literal("custom blocks: " + (mine.isEmpty() ? "none" : String.join(", ", mine))), false);
        return 1;
    }
}
