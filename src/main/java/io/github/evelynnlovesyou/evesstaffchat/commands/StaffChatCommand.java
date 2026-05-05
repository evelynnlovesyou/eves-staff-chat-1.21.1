package io.github.evelynnlovesyou.evesstaffchat.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import io.github.evelynnlovesyou.evesstaffchat.config.ChatDefinition;
import io.github.evelynnlovesyou.evesstaffchat.config.ConfigRepository;
import io.github.evelynnlovesyou.evesstaffchat.exceptions.ConfigLoadException;
import io.github.evelynnlovesyou.evesstaffchat.manager.PermissionManager;
import io.github.evelynnlovesyou.evesstaffchat.manager.StaffChatManager;
import io.github.evelynnlovesyou.evesstaffchat.utils.TextUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;

public class StaffChatCommand {
    private static final String RELOAD_PERM = "evesstaffchat.reload";
    private static final Logger LOGGER = LoggerFactory.getLogger("eves-staff-chat");

    // tracks all currently registered commands, removed when reloading to prevent registration of deleted chats and to update permissions
    private static final Set<String> REGISTERED_CHAT_COMMANDS = new HashSet<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("evesstaffchat")
                .requires(source -> hasPermission(source, RELOAD_PERM))
                .then(
                    Commands.literal("reload")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            try {
                                ConfigRepository.get().reload();
                                syncChatCommands(source.getServer().getCommands().getDispatcher());
                                source.getServer().getPlayerList().getPlayers().forEach(p -> source.getServer().getCommands().sendCommands(p));
                                sendToSource(source, Component.literal(ConfigRepository.get().lang().get("reload_success")), false);
                                return 1;
                            } catch (ConfigLoadException e) {
                                LOGGER.error("Failed to reload staff chat configuration", e);
                                sendToSource(source, Component.literal(ConfigRepository.get().lang().get("reload_failed")), true);
                                return 0;
                            }
                        })
                )
        );
        syncChatCommands(dispatcher);
    }
    
    // sync the current config chats with registered commands
    private static void syncChatCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // remove all commands registered
        for (String name : REGISTERED_CHAT_COMMANDS) {
            removeCommandNode(dispatcher, name);
        }
        REGISTERED_CHAT_COMMANDS.clear();

        // register commands for all chats in the config
        for (ChatDefinition chat : ConfigRepository.get().chats().getChats().values()) {
            registerSingleChatCommand(dispatcher, chat);
            registerSingleToggleCommand(dispatcher, chat);
        }
    }

    // remove commands from dispatcher that are still loaded but aren't in config
    private static void removeCommandNode(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        try {
            Field childrenField = CommandNode.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, CommandNode<CommandSourceStack>> children =
                (Map<String, CommandNode<CommandSourceStack>>) childrenField.get(dispatcher.getRoot());
            children.remove(name);

            Field literalsField = CommandNode.class.getDeclaredField("literals");
            literalsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, LiteralCommandNode<CommandSourceStack>> literals =
                (Map<String, LiteralCommandNode<CommandSourceStack>>) literalsField.get(dispatcher.getRoot());
            literals.remove(name);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Failed to unregister command /{}: {}", name, e.getMessage());
        }
    }

    // register individual chat commands
    private static void registerSingleChatCommand(CommandDispatcher<CommandSourceStack> dispatcher, ChatDefinition chat) {
        REGISTERED_CHAT_COMMANDS.add(chat.commandName());

        dispatcher.register(
            Commands.literal(chat.commandName())
                .requires(source -> hasPermission(source, chat.permissionBase() + ".use"))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!PermissionManager.hasPermission(player, chat.permissionBase() + ".toggle")) {
                        sendMessage(player, Component.literal(TextUtil.applyPlaceholders(ConfigRepository.get().lang().get("no_permission_toggle"), "%chat_name%", chat.chatName(), "%chat_key%", chat.key())));
                        return 0;
                    }
                    boolean enabled = StaffChatManager.toggle(player, chat.key());
                    sendMessage(player, Component.literal(enabled
                        ? TextUtil.applyPlaceholders(ConfigRepository.get().lang().get("chat_enabled"), "%chat_name%", chat.chatName(), "%chat_key%", chat.key())
                        : TextUtil.applyPlaceholders(ConfigRepository.get().lang().get("chat_disabled"), "%chat_name%", chat.chatName(), "%chat_key%", chat.key())));
                    return 1;
                })
                .then(
                    Commands.argument("message", StringArgumentType.greedyString())
                        .requires(source -> hasPermission(source, chat.permissionBase() + ".send"))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                            String message = getString(ctx, "message");
                            StaffChatManager.sendStaffMessage(player, chat.key(), message);
                            return 1;
                        })
                )
        );
    }

    // register toggle command
    private static void registerSingleToggleCommand(CommandDispatcher<CommandSourceStack> dispatcher, ChatDefinition chat) {
        String commandName = chat.commandName() + "toggle";
        REGISTERED_CHAT_COMMANDS.add(commandName);

        dispatcher.register(
            Commands.literal(commandName)
                .requires(source -> hasPermission(source, chat.permissionBase() + ".toggle"))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();

                    boolean enabled = StaffChatManager.toggle(player, chat.key());
                    sendMessage(player, Component.literal(enabled
                        ? TextUtil.applyPlaceholders(ConfigRepository.get().lang().get("chat_enabled"), "%chat_name%", chat.chatName(), "%chat_key%", chat.key())
                        : TextUtil.applyPlaceholders(ConfigRepository.get().lang().get("chat_disabled"), "%chat_name%", chat.chatName(), "%chat_key%", chat.key())));
                    return 1;
                })
        );
    }

    // send message to player - actionbar or chat depending on config
    private static void sendMessage(ServerPlayer player, Component component) {
        if (ConfigRepository.get().config().useActionBar()) {
            player.displayClientMessage(component, true); // haha action bar go brr
        } else {
            player.sendSystemMessage(component);
        }
    }

    // send message to command source
    private static void sendToSource(CommandSourceStack source, Component component, boolean error) {
        if (error) {
            source.sendFailure(component);
        } else {
            source.sendSuccess(() -> component, false);
        }
    }

    // check perms
    private static boolean hasPermission(CommandSourceStack source, String permission) {
        return source.getEntity() instanceof ServerPlayer player
            && PermissionManager.hasPermission(player, permission);
    }
}
