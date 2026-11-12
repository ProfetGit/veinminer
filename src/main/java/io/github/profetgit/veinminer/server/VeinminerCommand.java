package io.github.profetgit.veinminer.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** {@code /veinminer} (toggle yourself, saved), {@code status}, and {@code reload}, {@code set} and {@code tune} (level 2). */
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
        return 1;
    }
}
