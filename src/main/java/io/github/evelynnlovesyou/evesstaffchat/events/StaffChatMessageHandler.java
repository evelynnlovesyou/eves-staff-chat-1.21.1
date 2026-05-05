package io.github.evelynnlovesyou.evesstaffchat.events;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

import io.github.evelynnlovesyou.evesstaffchat.config.ConfigRepository;
import io.github.evelynnlovesyou.evesstaffchat.manager.StaffChatManager;
import net.minecraft.network.chat.Component;

public class StaffChatMessageHandler {
    
    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender != null) {
                String activeChat = StaffChatManager.getPlayerToggledChat(sender);
                if (activeChat == null) {
                    return true;
                }

                var config = ConfigRepository.get();
                String rawMessage = message.signedContent();
                if (rawMessage.isEmpty()) {
                    String chatName = config.chats().getChat(activeChat).chatName();
                    Component feedback = Component.literal(config.lang().get("empty_message").replace("%chat_name%", chatName));
                    if (config.config().useActionBar()) {
                        sender.displayClientMessage(feedback, true);
                    } else {
                        sender.sendSystemMessage(feedback);
                    }
                } else {
                    StaffChatManager.sendStaffMessage(sender, activeChat, rawMessage);
                }
                return false;
            }
            return true;
        });
    }
}
