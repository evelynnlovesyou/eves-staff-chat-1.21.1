package io.github.evelynnlovesyou.evesstaffchat.events;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import io.github.evelynnlovesyou.evesstaffchat.manager.StaffChatManager;

public class StaffChatMessageHandler {

    // Utility class - prevent instantiation
    private StaffChatMessageHandler() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender != null && StaffChatManager.isPlayerToggled(sender)) {
                String rawMessage = message.signedContent();
                if (!rawMessage.isEmpty()) {
                    StaffChatManager.sendStaffMessage(sender, rawMessage);
                }
                return false;
            }
            return true;
        });
    }
}
