package io.github.evelynnlovesyou.evesstaffchat.events;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import io.github.evelynnlovesyou.evesstaffchat.manager.StaffChatManager;

public class PlayerConnectionHandler {

    public static void register() {
        // clean on disconnect to prevent memory leak
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (handler.player != null) {
                StaffChatManager.removeToggled(handler.player.getGameProfile().getId());
            }
        });
    }
}
