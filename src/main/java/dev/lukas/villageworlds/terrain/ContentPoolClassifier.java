package dev.lukas.villageworlds.terrain;

import dev.lukas.villageworlds.config.VillageWorldsConfig;

import java.util.Locale;

public final class ContentPoolClassifier {
    private ContentPoolClassifier() {}

    public enum ContentType {
        NONE,
        VILLAGE,
        JUNGLE_TEMPLE,
        WITCH,
        EXPLORATION
    }

    public static ContentType classify(String identifier) {
        String normalized = identifier.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("villageworlds:village/")) return ContentType.VILLAGE;
        if (normalized.startsWith("villageworlds:jungle_temple/")) return ContentType.JUNGLE_TEMPLE;
        if (normalized.startsWith("villageworlds:witch/")) return ContentType.WITCH;
        if (normalized.startsWith("villageworlds:exploration/")) return ContentType.EXPLORATION;
        return ContentType.NONE;
    }

    public static boolean enabled(ContentType type, VillageWorldsConfig config) {
        return switch (type) {
            case NONE -> true;
            case VILLAGE -> config.newVillageTypes();
            case JUNGLE_TEMPLE -> config.improvedJungleTemples();
            case WITCH -> config.improvedWitchStructures();
            case EXPLORATION -> config.additionalStructures();
        };
    }
}
