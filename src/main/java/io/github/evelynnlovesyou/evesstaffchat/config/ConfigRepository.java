package io.github.evelynnlovesyou.evesstaffchat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.evelynnlovesyou.evesstaffchat.config.files.*;
import io.github.evelynnlovesyou.evesstaffchat.exceptions.ConfigLoadException;

import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ConfigRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger("eves-staff-chat");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR_NAME = "evesstaffchat";

    private static ConfigRepository INSTANCE;

    private final LangFile langFile;
    private final ConfigFile configFile;
    private final ChatConfigFile chatConfigFile;

    private ConfigRepository(Path configFolder) {
        this.langFile = new LangFile(configFolder, GSON);
        this.configFile = new ConfigFile(configFolder, GSON);
        this.chatConfigFile = new ChatConfigFile(configFolder, GSON);
    }

    public static void init() throws ConfigLoadException {
        Path rootFolder = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_DIR_NAME);
        try {
            if (!Files.exists(rootFolder)) {
                Files.createDirectories(rootFolder);
                LOGGER.info("Created config folder: {}", rootFolder);
            }
        } catch (IOException e) {
            throw new ConfigLoadException("Failed to create config folder: " + e.getMessage(), e);
        }
        INSTANCE = new ConfigRepository(rootFolder);
        INSTANCE.reload();
    }

    // Returns the active repository instance 
    public static ConfigRepository get() {
        return INSTANCE;
    }

    // Reloads every registered config file from disk. 
    public void reload() throws ConfigLoadException {
        langFile.load();
        configFile.load();
        chatConfigFile.load();
    }

    public LangFile lang() {
        return langFile;
    }

    public ConfigFile config() {
        return configFile;
    }

    public ChatConfigFile chats() {
        return chatConfigFile;
    }

    // Package private helpers used by the individual file classes

    public static Path ensureFile(Path folder, String fileName, String defaultResource) throws IOException {
        return ensureFile(folder, fileName, defaultResource, null);
    }

    public static Path ensureFile(Path folder, String fileName, String defaultResource, String fallbackContent) throws IOException {
        Path file = folder.resolve(fileName);
        if (!Files.exists(file)) {
            try (InputStream is = ConfigRepository.class.getResourceAsStream(defaultResource)) {
                if (is != null) {
                    Files.copy(is, file);
                    LOGGER.info("Copied default {} to {}", fileName, file);
                } else {
                    if (fallbackContent != null) {
                        Files.writeString(file, fallbackContent, StandardCharsets.UTF_8);
                        LOGGER.info("created default values {} at {}", fileName, file);
                    } else {
                        LOGGER.warn("default resource {} not found in jar and no fallback provided", defaultResource);
                    }
                }
            }
        }
        return file;
    }

    public static <K, V> boolean mergeDefaults(Map<K, V> current, Map<K, V> defaults) {
        boolean updated = false;
        for (Map.Entry<K, V> entry : defaults.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                current.put(entry.getKey(), entry.getValue());
                updated = true;
            }
        }
        return updated;
    }
}

