package io.github.evelynnlovesyou.evesstaffchat.exceptions;

// exception thrown when there is an error loading the configs
public class ConfigLoadException extends Exception {

    public ConfigLoadException(String message) {
        super(message);
    }

    public ConfigLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
