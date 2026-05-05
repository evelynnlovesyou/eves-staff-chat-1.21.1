package io.github.evelynnlovesyou.evesstaffchat.events;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import io.github.evelynnlovesyou.evesstaffchat.manager.StaffChatManager;

public class StaffChatMessageHandler {

    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender != null) {
                String activeChat = StaffChatManager.getPlayerToggledChat(sender);
                if (activeChat == null) {
                    return true;
                }

                String rawMessage = message.signedContent();
                if (!rawMessage.isEmpty()) {
                    StaffChatManager.sendStaffMessage(sender, activeChat, rawMessage);
                }
                return false;
            }
            return true;
        });
    }
}
