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

public class ConfigFile {

    private static final Logger LOGGER = LoggerFactory.getLogger("eves-staff-chat");
    static final String FILE_NAME = "config.json";
    private static final String DEFAULT_RESOURCE = "/config/config.json";

    private static final Map<String, Object> DEFAULTS = Map.of(
        "use_action_bar", true
    );

    private final Path configFolder;
    private final Gson gson;
    private final Map<String, Object> settings = new HashMap<>();

    public ConfigFile(Path configFolder, Gson gson) {
        this.configFolder = configFolder;
        this.gson = gson;
    }

    public void load() throws ConfigLoadException {
        settings.clear();
        try {
            Path file = ConfigRepository.ensureFile(configFolder, FILE_NAME, DEFAULT_RESOURCE, gson.toJson(DEFAULTS));
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Map<String, Object> loaded = gson.fromJson(reader, type);
                if (loaded != null) {
                    settings.putAll(loaded);
                }
            }
            if (ConfigRepository.mergeDefaults(settings, DEFAULTS)) {
                save();
                LOGGER.info("Updated config.json with missing keys");
            }
        } catch (IOException e) {
            throw new ConfigLoadException("Failed to load config.json: " + e.getMessage(), e);
        }
    }

    public boolean useActionBar() {
        return Boolean.parseBoolean(String.valueOf(settings.getOrDefault("use_action_bar", true)));
    }

    private void save() throws IOException {
        Path file = configFolder.resolve(FILE_NAME);
        Files.writeString(file, gson.toJson(settings));
        LOGGER.info("Saved config.json to {}", file);
    }
}
