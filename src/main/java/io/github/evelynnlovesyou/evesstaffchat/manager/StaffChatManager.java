package io.github.evelynnlovesyou.evesstaffchat.manager;

import io.github.evelynnlovesyou.evesstaffchat.config.ChatDefinition;
import io.github.evelynnlovesyou.evesstaffchat.config.ConfigRepository;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StaffChatManager {

    private static final Map<UUID, String> ACTIVE_CHAT = new ConcurrentHashMap<>();

    public static void removeToggled(UUID playerId) {
        ACTIVE_CHAT.remove(playerId);
    }

    public static boolean toggle(ServerPlayer player, String chatKey) {
        if (ConfigRepository.get().chats().getChat(chatKey) == null) {
            return false;
        }

        UUID id = player.getGameProfile().getId();
        String current = ACTIVE_CHAT.get(id);
        if (chatKey.equals(current)) {
            ACTIVE_CHAT.remove(id);
            return false;
        } else {
            ACTIVE_CHAT.put(id, chatKey);
            return true;
        }
    }

    public static String getPlayerToggledChat(ServerPlayer player) {
        UUID id = player.getGameProfile().getId();
        String chatKey = ACTIVE_CHAT.get(id);
        if (chatKey != null && ConfigRepository.get().chats().getChat(chatKey) == null) {
            ACTIVE_CHAT.remove(id);
            return null;
        }
        return chatKey;
    }

    public static void sendStaffMessage(ServerPlayer sender, String chatKey, String message) {
        if (sender == null || sender.server == null) return;

        ChatDefinition chat = ConfigRepository.get().chats().getChat(chatKey);
        if (chat == null) return;

        String playerName = sender.getGameProfile().getName();
        String formattedMessage = chat.messageFormat()
                .replace("%chat%", chat.key())
                .replace("%player%", playerName)
                .replace("%message%", message);
        
        Component comp = Component.literal(formattedMessage);

        for (ServerPlayer p : sender.server.getPlayerList().getPlayers()) {
            if (canReceiveStaffMessages(p, chat.permissionBase())) {
                p.sendSystemMessage(comp);
            }
        }
    }

    public static boolean canReceiveStaffMessages(ServerPlayer player, String permissionBase) {
        return PermissionManager.hasPermission(player, permissionBase + ".receive");
    }
}

