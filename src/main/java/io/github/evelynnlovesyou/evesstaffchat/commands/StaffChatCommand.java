package io.github.evelynnlovesyou.evesstaffchat.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import io.github.evelynnlovesyou.evesstaffchat.manager.StaffChatManager;
import io.github.evelynnlovesyou.evesstaffchat.config.ModConfig;
import io.github.evelynnlovesyou.evesstaffchat.exceptions.ConfigLoadException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;

public class StaffChatCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger("eves-staff-chat");

    // Utility class - prevent instantiation
    private StaffChatCommand() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal(ModConfig.COMMAND)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();

                    if (!canUseToggle(player)) {
                        sendMessage(player, Component.literal(ModConfig.NO_PERMISSION_TOGGLE));
                        return 0;
                    }

                    boolean enabled = StaffChatManager.toggle(player);
                    sendMessage(player, Component.literal(enabled ? ModConfig.STAFFCHAT_ENABLED : ModConfig.STAFFCHAT_DISABLED));
                    return 1;
                })
                .then(
                    Commands.argument(ModConfig.ARG_MESSAGE, StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                            if (!canUseSend(player)) {
                                sendMessage(player, Component.literal(ModConfig.NO_PERMISSION_SEND));
                                return 0;
                            }

                            String message = getString(ctx, ModConfig.ARG_MESSAGE);
                            StaffChatManager.sendStaffMessage(player, message);
                            return 1;
                        })
                )
        );

        // Register toggle-only command
        dispatcher.register(
            Commands.literal(ModConfig.COMMAND_TOGGLE)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();

                    if (!canUseToggle(player)) {
                        sendMessage(player, Component.literal(ModConfig.NO_PERMISSION_TOGGLE));
                        return 0;
                    }

                    boolean enabled = StaffChatManager.toggle(player);
                    sendMessage(player, Component.literal(enabled ? ModConfig.STAFFCHAT_ENABLED : ModConfig.STAFFCHAT_DISABLED));
                    return 1;
                })
        );

        // Register admin command: /evesstaffchat reload
        dispatcher.register(
            Commands.literal("evesstaffchat")
                .then(
                    Commands.literal("reload")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();

                            if (!canUseReload(source)) {
                                sendToSource(source, Component.literal(ModConfig.NO_PERMISSION_RELOAD), true);
                                return 0;
                            }

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

    // Check if player can toggle staff chat
    private static boolean canUseToggle(ServerPlayer player) {
        return StaffChatManager.hasPermission(player, ModConfig.PERM_TOGGLE);
    }

    // Check if player can send one-off staff messages
    private static boolean canUseSend(ServerPlayer player) {
        return StaffChatManager.hasPermission(player, ModConfig.PERM_SEND);
    }

    private static boolean canUseReload(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return StaffChatManager.hasPermission(player, ModConfig.PERM_RELOAD);
        }
        return true;
    }
}
