package io.github.evelynnlovesyou.evesstaffchat.manager;

import io.github.evelynnlovesyou.evesstaffchat.config.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StaffChatManager {

    private static final Set<UUID> TOGGLED = ConcurrentHashMap.newKeySet();

    // Utility class - prevent instantiation
    private StaffChatManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void removeToggled(UUID playerId) {
        TOGGLED.remove(playerId);
    }

    public static boolean toggle(ServerPlayer player) {
        UUID id = player.getGameProfile().getId();
        if (TOGGLED.contains(id)) {
            TOGGLED.remove(id);
            return false;
        } else {
            TOGGLED.add(id);
            return true;
        }
    }

    public static boolean isPlayerToggled(ServerPlayer player) {
        return TOGGLED.contains(player.getGameProfile().getId());
    }

    public static void sendStaffMessage(ServerPlayer sender, String message) {
        if (sender == null || sender.server == null) return;

        String playerName = sender.getGameProfile().getName();
        String formattedMessage = ModConfig.STAFF_MESSAGE_FORMAT
                .replace("%player%", playerName)
                .replace("%message%", message);
        
        Component comp = Component.literal(formattedMessage);

        for (ServerPlayer p : sender.server.getPlayerList().getPlayers()) {
            if (canReceiveStaffMessages(p)) {
                p.sendSystemMessage(comp);
            }
        }
    }

    public static boolean canReceiveStaffMessages(ServerPlayer player) {
        return PermissionManager.hasPermission(player, ModConfig.PERM_RECEIVE);
    }
}

