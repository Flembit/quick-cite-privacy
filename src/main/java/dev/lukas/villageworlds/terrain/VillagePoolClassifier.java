package dev.lukas.villageworlds.terrain;

import java.util.Locale;

public final class VillagePoolClassifier {
    private VillagePoolClassifier() {}

    public static boolean isVillagePool(String identifier) {
        String normalized = identifier.toLowerCase(Locale.ROOT);
        return normalized.startsWith("minecraft:village/")
                || normalized.startsWith("village/")
                || normalized.contains(":village/")
                || normalized.contains("/village/");
    }
}
