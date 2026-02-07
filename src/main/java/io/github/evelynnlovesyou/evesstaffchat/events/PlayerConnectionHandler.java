package io.github.evelynnlovesyou.evesstaffchat.events;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import io.github.evelynnlovesyou.evesstaffchat.manager.StaffChatManager;

public class PlayerConnectionHandler {

    // Utility class - prevent instantiation
    private PlayerConnectionHandler() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void register() {
        // Clean up toggled state when player disconnects (prevents memory leak)
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (handler.player != null) {
                StaffChatManager.removeToggled(handler.player.getGameProfile().getId());
            }
        });
    }
}
