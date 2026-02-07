package io.github.evelynnlovesyou.evesstaffchat.exceptions;

public class ConfigLoadException extends Exception {
    public ConfigLoadException(String message) {
        super(message);
    }

    public ConfigLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
