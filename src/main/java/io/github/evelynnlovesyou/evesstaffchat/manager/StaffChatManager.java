package io.github.evelynnlovesyou.evesstaffchat.manager;

import io.github.evelynnlovesyou.evesstaffchat.config.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaffChatManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("eves-staff-chat");
    private static final Set<UUID> TOGGLED = ConcurrentHashMap.newKeySet();
    private static volatile LuckPerms luckPermsApi;
    private static volatile boolean luckPermsMissing = false;
    private static volatile boolean luckPermsWarned = false;

    // Utility class - prevent instantiation
    private StaffChatManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void init() {
        initLuckPerms();
    }

    private static synchronized void initLuckPerms() {
        if (luckPermsApi != null || luckPermsMissing) {
            return;
        }

        try {
            luckPermsApi = LuckPermsProvider.get();
            LOGGER.info("Successfully detected and initialized LuckPerms API");
        } catch (NoClassDefFoundError ignored) {
            luckPermsMissing = true;
            if (!luckPermsWarned) {
                LOGGER.warn("LuckPerms not detected - using fallback permission system (OP only)");
                luckPermsWarned = true;
            }
        } catch (Exception ignored) {
            if (!luckPermsWarned) {
                LOGGER.warn("LuckPerms unavailable - will retry when available");
                luckPermsWarned = true;
            }
        }
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
        return hasPermission(player, ModConfig.PERM_RECEIVE);
    }

    public static boolean hasPermission(ServerPlayer player, String permission) {
        initLuckPerms();
        if (luckPermsApi != null) {
            try {
                User user = luckPermsApi.getUserManager().getUser(player.getGameProfile().getId());
                if (user != null && user.getCachedData().getPermissionData()
                        .checkPermission(permission).asBoolean()) {
                    return true;
                }
            } catch (Exception ignored) {
                // Fallback to OP
            }
        }
        return player.server != null && player.server.getPlayerList().isOp(player.getGameProfile());
    }
}

