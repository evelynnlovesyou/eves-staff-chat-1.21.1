package io.github.evelynnlovesyou.evesstaffchat.utils;

// Util class to make shit easier
public class TextUtil {

    public static String applyPlaceholders(String text, String... placeholders) {
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String key = placeholders[i];
            String value = placeholders[i + 1];
            text = text.replace(key, value);
        }
        return text;
    }
}
