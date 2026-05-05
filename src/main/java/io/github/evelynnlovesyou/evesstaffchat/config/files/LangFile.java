package io.github.evelynnlovesyou.evesstaffchat.config.files;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.github.evelynnlovesyou.evesstaffchat.config.ConfigRepository;
import io.github.evelynnlovesyou.evesstaffchat.exceptions.ConfigLoadException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LangFile {

    private static final Logger LOGGER = LoggerFactory.getLogger("eves-staff-chat");
    static final String FILE_NAME = "lang.json";
    private static final String DEFAULT_RESOURCE = "/config/lang.json";

    private static final Map<String, String> DEFAULTS = Map.of(
        "no_permission_toggle", "no perms to toggle %chat_name%",
        "no_permission_send",   "no perms to send to %chat_name%",
        "no_permission_reload", "no perms to reload config",
        "chat_enabled",         "%chat_name% enabled",
        "chat_disabled",        "%chat_name% disabled",
        "reload_success",       "reloaded",
        "reload_failed",        "failed to reload config"
    );

    private final Path configFolder;
    private final Gson gson;
    private final Map<String, String> messages = new HashMap<>();

    public LangFile(Path configFolder, Gson gson) {
        this.configFolder = configFolder;
        this.gson = gson;
    }

    public void load() throws ConfigLoadException {
        messages.clear();
        try {
            Path file = ConfigRepository.ensureFile(configFolder, FILE_NAME, DEFAULT_RESOURCE, gson.toJson(DEFAULTS));
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Map<String, String> loaded = gson.fromJson(reader, type);
                if (loaded != null) {
                    messages.putAll(loaded);
                }
            }
            if (ConfigRepository.mergeDefaults(messages, DEFAULTS)) {
                save();
                LOGGER.info("Updated lang.json with missing keys");
            }
        } catch (IOException e) {
            throw new ConfigLoadException("Failed to load lang.json: " + e.getMessage(), e);
        }
    }

    public String get(String key) {
        return messages.getOrDefault(key, "");
    }

    private void save() throws IOException {
        Path file = configFolder.resolve(FILE_NAME);
        Files.writeString(file, gson.toJson(messages));
        LOGGER.info("Saved lang.json to {}", file);
    }
}
