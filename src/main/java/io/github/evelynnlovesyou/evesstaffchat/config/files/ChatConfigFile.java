package io.github.evelynnlovesyou.evesstaffchat.config.files;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.github.evelynnlovesyou.evesstaffchat.config.ChatDefinition;
import io.github.evelynnlovesyou.evesstaffchat.config.ConfigRepository;
import io.github.evelynnlovesyou.evesstaffchat.exceptions.ConfigLoadException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChatConfigFile {

    private static final Logger LOGGER = LoggerFactory.getLogger("eves-staff-chat");
    static final String FILE_NAME = "chats.json";
    private static final String DEFAULT_RESOURCE = "/config/chats.json";

    private static final String DEFAULT_MESSAGE_FORMAT = "[Staff] %player%: %message%";
    private static final Map<String, Map<String, String>> DEFAULT_CHATS = createDefaultChats();

    private final Path configFolder;
    private final Gson gson;
    private final Map<String, ChatDefinition> chatDefinitions = new LinkedHashMap<>();

    public ChatConfigFile(Path configFolder, Gson gson) {
        this.configFolder = configFolder;
        this.gson = gson;
    }

    public void load() throws ConfigLoadException {
        chatDefinitions.clear();
        try {
            Path file = ConfigRepository.ensureFile(configFolder, FILE_NAME, DEFAULT_RESOURCE, gson.toJson(DEFAULT_CHATS));
            Type type = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
            Map<String, Map<String, String>> chatData = new LinkedHashMap<>();
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Map<String, Map<String, String>> loaded = gson.fromJson(reader, type);
                if (loaded != null) {
                    chatData.putAll(loaded);
                }
            }

            if (ensureStaffChatDefault(chatData)) {
                save(chatData);
                LOGGER.info("Updated chats.json with missing default staffchat entry");
            }

            for (Map.Entry<String, Map<String, String>> entry : chatData.entrySet()) {
                String key = sanitizeKey(entry.getKey());
                if (key == null) {
                    LOGGER.warn("Skipping invalid chat key: '{}'", entry.getKey());
                    continue;
                }
                Map<String, String> values = entry.getValue() == null ? Map.of() : entry.getValue();
                String permissionBase = values.getOrDefault("permission_base", "evesstaffchat." + key).trim();
                if (permissionBase.isEmpty()) {
                    permissionBase = "evesstaffchat." + key;
                }
                String chatcommand = sanitizeKey(values.getOrDefault("chat_command", key).trim());
                if (chatcommand == null) {
                    LOGGER.warn("Invalid chat_command for key '{}', falling back to key name", key);
                    chatcommand = key;
                }
                String messageFormat = values.getOrDefault("message_format", DEFAULT_MESSAGE_FORMAT);
                String chatname = values.getOrDefault("chat_name", key).trim();
                if (chatname.isEmpty()) {
                    chatname = key;
                }
                chatDefinitions.put(key, new ChatDefinition(key, chatname, chatcommand, permissionBase, messageFormat));
            }
        } catch (IOException e) {
            throw new ConfigLoadException("Failed to load chats.json: " + e.getMessage(), e);
        }
    }

    public Map<String, ChatDefinition> getChats() {
        return Collections.unmodifiableMap(chatDefinitions);
    }

    public ChatDefinition getChat(String key) {
        return chatDefinitions.get(key);
    }

    private void save(Map<String, Map<String, String>> data) throws IOException {
        Path file = configFolder.resolve(FILE_NAME);
        Files.writeString(file, gson.toJson(data));
        LOGGER.info("Saved chats.json to {}", file);
    }

    private static boolean ensureStaffChatDefault(Map<String, Map<String, String>> chats) {
        Map<String, String> staffChatDefaults = DEFAULT_CHATS.get("staffchat");
        if (!chats.containsKey("staffchat")) {
            chats.put("staffchat", new LinkedHashMap<>(staffChatDefaults));
            return true;
        }
        Map<String, String> staffChat = chats.get("staffchat");
        if (staffChat == null) {
            chats.put("staffchat", new LinkedHashMap<>(staffChatDefaults));
            return true;
        }
        return ConfigRepository.mergeDefaults(staffChat, staffChatDefaults);
    }

    private static Map<String, Map<String, String>> createDefaultChats() {
        Map<String, Map<String, String>> defaults = new LinkedHashMap<>();
        Map<String, String> staffchat = new LinkedHashMap<>();
        staffchat.put("permission_base", "evesstaffchat.staffchat");
        staffchat.put("message_format", DEFAULT_MESSAGE_FORMAT);
        staffchat.put("chat_command", "staffchat");
        staffchat.put("chat_name", "Staff Chat");
        defaults.put("staffchat", staffchat);
        return defaults;
    }

    private static String sanitizeKey(String input) {
        if (input == null) return null;
        String candidate = input.trim().toLowerCase();
        if (candidate.isEmpty() || !candidate.matches("[a-z0-9_]+")) return null;
        return candidate;
    }
}
