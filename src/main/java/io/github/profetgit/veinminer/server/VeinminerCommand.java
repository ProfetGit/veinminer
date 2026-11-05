package io.github.profetgit.veinminer.server;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** {@code /veinminer} (toggle yourself), {@code status}, and {@code reload} (level 2). */
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
            })));
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
            + ", durability " + v.durability() + ", diagonal " + v.diagonal() + ", visit_cap " + v.visitCap()), false);
        src.sendSuccess(() -> Component.literal(Groups.report()), false);
        return 1;
    }
}
