package io.github.evelynnlovesyou.evesstaffchat.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import io.github.evelynnlovesyou.evesstaffchat.manager.PermissionManager;
import io.github.evelynnlovesyou.evesstaffchat.manager.StaffChatManager;
import io.github.evelynnlovesyou.evesstaffchat.config.ModConfig;
import io.github.evelynnlovesyou.evesstaffchat.exceptions.ConfigLoadException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;

public class StaffChatCommand {
    private static final String basePerm = "evesstaffchat.staffchat";
    private static final Logger LOGGER = LoggerFactory.getLogger("eves-staff-chat");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("staffchat")
                .requires(source -> hasPermission(source, basePerm + ".use"))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();

                    if (!PermissionManager.hasPermission(player, basePerm + ".toggle")) {
                        sendMessage(player, Component.literal(ModConfig.NO_PERMISSION_TOGGLE));
                        return 0;
                    }

                    boolean enabled = StaffChatManager.toggle(player);
                    sendMessage(player, Component.literal(enabled ? ModConfig.STAFFCHAT_ENABLED : ModConfig.STAFFCHAT_DISABLED));
                    return 1;
                })
                .then(
                    Commands.argument("message", StringArgumentType.greedyString())
                        .requires(source -> hasPermission(source, basePerm + ".send"))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                            String message = getString(ctx, "message");
                            StaffChatManager.sendStaffMessage(player, message);
                            return 1;
                        })
                )
        );

        // Register toggle command: /staffchattoggle
        dispatcher.register(
            Commands.literal("staffchattoggle")
                .requires(source -> hasPermission(source, basePerm + ".toggle"))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();

                    boolean enabled = StaffChatManager.toggle(player);
                    sendMessage(player, Component.literal(enabled ? ModConfig.STAFFCHAT_ENABLED : ModConfig.STAFFCHAT_DISABLED));
                    return 1;
                })
        );

        // Register admin command: /evesstaffchat reload
        dispatcher.register(
            Commands.literal("evesstaffchat")
            .requires(source -> hasPermission(source, basePerm + ".reload"))
                .then(
                    Commands.literal("reload")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            try {
                                ModConfig.reload();
                                sendToSource(source, Component.literal(ModConfig.RELOAD_SUCCESS), false);
                                return 1;
                            } catch (ConfigLoadException e) {
                                LOGGER.error("Failed to reload staff chat configuration", e);
                                sendToSource(source, Component.literal(ModConfig.RELOAD_FAILED), true);
                                return 0;
                            }
                        })
                )
        );
    }

    private static void sendMessage(ServerPlayer player, Component component) {
        if (ModConfig.USE_ACTION_BAR) {
            player.displayClientMessage(component, true);
        } else {
            player.sendSystemMessage(component);
        }
    }

    private static void sendToSource(CommandSourceStack source, Component component, boolean error) {
        if (source.getEntity() instanceof ServerPlayer player) {
            sendMessage(player, component);
            return;
        }
        if (error) {
            source.sendFailure(component);
        } else {
            source.sendSuccess(() -> component, false);
        }
    }

    private static boolean hasPermission(CommandSourceStack source, String permission) {
        return source.getEntity() instanceof ServerPlayer player
            && PermissionManager.hasPermission(player, permission);
    }
}
