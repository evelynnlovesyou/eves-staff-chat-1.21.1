package io.github.evelynnlovesyou.evesstaffchat.manager;

import io.github.evelynnlovesyou.evesstaffchat.config.ChatDefinition;
import io.github.evelynnlovesyou.evesstaffchat.config.ConfigRepository;
import io.github.evelynnlovesyou.evesstaffchat.utils.TextUtil;
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
        boolean[] result = {false};
        ACTIVE_CHAT.compute(id, (k, current) -> {
            if (chatKey.equals(current)) {
                result[0] = false;
                return null;
            } else {
                result[0] = true;
                return chatKey;
            }
        });
        return result[0];
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
        String formattedMessage = TextUtil.applyPlaceholders(chat.messageFormat(), "%player%", playerName, "%message%", message, "%chat_name%", chat.chatName(), "%chat_key%", chat.key());
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

