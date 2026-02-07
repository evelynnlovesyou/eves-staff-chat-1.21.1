package io.github.evelynnlovesyou.evesstaffchat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.evelynnlovesyou.evesstaffchat.exceptions.ConfigLoadException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ModConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("eves-staff-chat");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String DEFAULT_LANG_PATH = "/config/lang.json";
    private static final String DEFAULT_CONFIG_PATH = "/config/config.json";

    private static final Map<String, String> MESSAGES = new HashMap<>();
    private static final Map<String, Object> SETTINGS = new HashMap<>();

    // Permission nodes (hardcoded)
    private static final String PERM_NODE_TOGGLE = "evesstaffchat.staffchat.toggle";
    private static final String PERM_NODE_SEND = "evesstaffchat.staffchat.send";
    private static final String PERM_NODE_RECEIVE = "evesstaffchat.staffchat.receive";
    private static final String PERM_NODE_RELOAD = "evesstaffchat.staffchat.reload";

    // Commands and arguments (hardcoded)
    private static final String CMD_STAFFCHAT = "staffchat";
    private static final String CMD_STAFFCHAT_TOGGLE = "staffchattoggle";
    private static final String ARG_MSG = "message";

    // Default values
    private static final Map<String, Object> DEFAULT_SETTINGS = Map.ofEntries(
        Map.entry("use_action_bar", true)
    );

    private static final Map<String, String> DEFAULT_MESSAGES = Map.ofEntries(
        Map.entry("no_permission_toggle", "no perms to toggle staff chat"),
        Map.entry("no_permission_send", "no perms to send to staff chat"),
        Map.entry("no_permission_reload", "no perms to reload config"),
        Map.entry("staffchat_enabled", "enabled staff chat"),
        Map.entry("staffchat_disabled", "disabled staff chat"),
        Map.entry("staff_message_format", "[Staff] %player%: %message%"),
        Map.entry("reload_success", "config reloaded"),
        Map.entry("reload_failed", "failed to reload config")
    );

    // Messages
    public static String NO_PERMISSION_TOGGLE;
    public static String NO_PERMISSION_SEND;
    public static String NO_PERMISSION_RELOAD;
    public static String STAFFCHAT_ENABLED;
    public static String STAFFCHAT_DISABLED;
    public static String STAFF_MESSAGE_FORMAT;
    public static String RELOAD_SUCCESS;
    public static String RELOAD_FAILED;

    // Settings
    public static String COMMAND;
    public static String COMMAND_TOGGLE;
    public static String ARG_MESSAGE;
    public static String PERM_TOGGLE;
    public static String PERM_SEND;
    public static String PERM_RECEIVE;
    public static String PERM_RELOAD;
    public static boolean USE_ACTION_BAR;

    static {
        try {
            load();
        } catch (ConfigLoadException e) {
            LOGGER.error("Failed to load mod configuration: {}", e.getMessage());
        }
    }

    private static Path getConfigFolder() throws IOException {
        Path folder = FabricLoader.getInstance().getConfigDir().resolve("evesstaffchat");
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
            LOGGER.info("Created config folder: {}", folder);
        }
        return folder;
    }

    private static Path getConfigFile(String fileName, String defaultResourcePath) throws IOException {
        Path file = getConfigFolder().resolve(fileName);
        if (!Files.exists(file)) {
            try (InputStream is = ModConfig.class.getResourceAsStream(defaultResourcePath)) {
                if (is != null) {
                    Files.copy(is, file);
                    LOGGER.info("Copied default {} to {}", fileName, file);
                } else {
                    LOGGER.warn("Default resource {} not found in jar!", defaultResourcePath);
                }
            }
        }
        return file;
    }

    private static void load() throws ConfigLoadException {
        try {
            MESSAGES.clear();
            SETTINGS.clear();

            // Load config files
            Path langFile = getConfigFile("lang.json", DEFAULT_LANG_PATH);
            Type langType = new TypeToken<Map<String, String>>() {}.getType();
            try (var reader = Files.newBufferedReader(langFile, StandardCharsets.UTF_8)) {
                Map<String, String> langMap = GSON.fromJson(reader, langType);
                if (langMap != null) {
                    MESSAGES.putAll(langMap);
                }
            }

            Path configFile = getConfigFile("config.json", DEFAULT_CONFIG_PATH);
            Type configType = new TypeToken<Map<String, Object>>() {}.getType();
            try (var reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                Map<String, Object> configMap = GSON.fromJson(reader, configType);
                if (configMap != null) {
                    SETTINGS.putAll(configMap);
                }
            }

            // Merge with defaults and save if updated
            boolean settingsUpdated = mergeWithDefaults(SETTINGS, DEFAULT_SETTINGS);
            boolean messagesUpdated = mergeWithDefaults(MESSAGES, DEFAULT_MESSAGES);

            if (settingsUpdated) {
                try {
                    saveSettings();
                    LOGGER.info("Updated config.json with missing keys");
                } catch (IOException e) {
                    LOGGER.warn("Failed to save updated config.json: {}", e.getMessage());
                }
            }
            if (messagesUpdated) {
                try {
                    saveMessages();
                    LOGGER.info("Updated lang.json with missing messages");
                } catch (IOException e) {
                    LOGGER.warn("Failed to save updated lang.json: {}", e.getMessage());
                }
            }

            // Populate public fields
            COMMAND = CMD_STAFFCHAT;
            COMMAND_TOGGLE = CMD_STAFFCHAT_TOGGLE;
            ARG_MESSAGE = ARG_MSG;
            PERM_TOGGLE = PERM_NODE_TOGGLE;
            PERM_SEND = PERM_NODE_SEND;
            PERM_RECEIVE = PERM_NODE_RECEIVE;
            PERM_RELOAD = PERM_NODE_RELOAD;
            USE_ACTION_BAR = Boolean.parseBoolean(String.valueOf(SETTINGS.get("use_action_bar")));

            NO_PERMISSION_TOGGLE = MESSAGES.get("no_permission_toggle");
            NO_PERMISSION_SEND = MESSAGES.get("no_permission_send");
            NO_PERMISSION_RELOAD = MESSAGES.get("no_permission_reload");
            STAFFCHAT_ENABLED = MESSAGES.get("staffchat_enabled");
            STAFFCHAT_DISABLED = MESSAGES.get("staffchat_disabled");
            STAFF_MESSAGE_FORMAT = MESSAGES.get("staff_message_format");
            RELOAD_SUCCESS = MESSAGES.get("reload_success");
            RELOAD_FAILED = MESSAGES.get("reload_failed");

            LOGGER.info("Mod configuration fully initialized");

        } catch (IOException e) {
            throw new ConfigLoadException("Failed to load configuration files: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ConfigLoadException("Failed to parse configuration: " + e.getMessage(), e);
        }
    }

    private static <K, V> boolean mergeWithDefaults(Map<K, V> current, Map<K, V> defaults) {
        boolean updated = false;
        for (Map.Entry<K, V> entry : defaults.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                current.put(entry.getKey(), entry.getValue());
                updated = true;
            }
        }
        return updated;
    }

    public static void saveSettings() throws IOException {
        Path configFile = getConfigFolder().resolve("config.json");
        Files.writeString(configFile, GSON.toJson(SETTINGS));
        LOGGER.info("Saved config.json to {}", configFile);
    }

    public static void saveMessages() throws IOException {
        Path langFile = getConfigFolder().resolve("lang.json");
        Files.writeString(langFile, GSON.toJson(MESSAGES));
        LOGGER.info("Saved lang.json to {}", langFile);
    }

    public static void reload() throws ConfigLoadException {
        load();
    }

    public static String get(String key) {
        return MESSAGES.get(key);
    }

    public static void setSetting(String key, Object value) {
        SETTINGS.put(key, value);
    }

    public static Object getSetting(String key) {
        return SETTINGS.get(key);
    }
}
