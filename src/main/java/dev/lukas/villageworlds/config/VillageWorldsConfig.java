package dev.lukas.villageworlds.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lukas.villageworlds.VillageWorldsMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public record VillageWorldsConfig(
        boolean terrainAwareVillages,
        int sampleRadius,
        int sampleStep,
        int maxElevationRange,
        int maxAdjacentDelta,
        double minimumFlatRatio,
        double maximumRoughness
) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static VillageWorldsConfig defaults() {
        return new VillageWorldsConfig(true, 40, 8, 13, 5, 0.70, 4.5);
    }

    public VillageWorldsConfig validated() {
        return new VillageWorldsConfig(
                terrainAwareVillages,
                clamp(sampleRadius, 16, 96),
                clamp(sampleStep, 4, 16),
                clamp(maxElevationRange, 4, 32),
                clamp(maxAdjacentDelta, 2, 12),
                clamp(minimumFlatRatio, 0.25, 1.0),
                clamp(maximumRoughness, 0.5, 16.0)
        );
    }

    public static VillageWorldsConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("villageworlds.json");
        VillageWorldsConfig fallback = defaults();
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                write(path, fallback);
                return fallback;
            }
            try (Reader reader = Files.newBufferedReader(path)) {
                VillageWorldsConfig parsed = GSON.fromJson(reader, VillageWorldsConfig.class);
                VillageWorldsConfig result = parsed == null ? fallback : parsed.validated();
                write(path, result);
                return result;
            }
        } catch (Exception exception) {
            VillageWorldsMod.LOGGER.error("Could not load {}; using safe defaults", path, exception);
            return fallback;
        }
    }

    private static void write(Path path, VillageWorldsConfig config) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(config, writer);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
