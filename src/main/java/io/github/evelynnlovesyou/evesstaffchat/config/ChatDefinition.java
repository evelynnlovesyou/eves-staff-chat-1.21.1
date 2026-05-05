package io.github.evelynnlovesyou.evesstaffchat.config;

// Chat definition used to store specific config data for each chat created
public final class ChatDefinition {

    private final String key;
    private final String commandName;
    private final String permissionBase;
    private final String messageFormat;

    public ChatDefinition(String key, String commandName, String permissionBase, String messageFormat) {
        this.key = key;
        this.commandName = commandName;
        this.permissionBase = permissionBase;
        this.messageFormat = messageFormat;
    }

    public String key() {
        return key;
    }

    public String commandName() {
        return commandName;
    }

    public String permissionBase() {
        return permissionBase;
    }

    public String messageFormat() {
        return messageFormat;
    }
}
