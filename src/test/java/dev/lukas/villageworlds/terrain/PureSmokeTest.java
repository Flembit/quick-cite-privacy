package dev.lukas.villageworlds.terrain;

import dev.lukas.villageworlds.config.VillageWorldsConfig;

public final class PureSmokeTest {
    public static void main(String[] args) throws Exception {
        TerrainThresholds defaults = new TerrainThresholds(13, 5, 0.70, 4.5);
        TerrainReport flat = TerrainAnalyzer.analyze(0, 0, 16, 8, (x, z) -> 70);
        check(TerrainAnalyzer.isAcceptable(flat, defaults), "flat terrain must pass");
        TerrainReport gentle = TerrainAnalyzer.analyze(0, 0, 16, 8, (x, z) -> 70 + Math.floorDiv(x + 16, 16));
        check(TerrainAnalyzer.isAcceptable(gentle, defaults), "gentle terrain must pass");
        TerrainReport cliff = TerrainAnalyzer.analyze(0, 0, 16, 8, (x, z) -> x < 0 ? 64 : 92);
        check(!TerrainAnalyzer.isAcceptable(cliff, defaults), "cliff terrain must fail");
        check(cliff.elevationRange() == 28, "cliff range must be measured");

        check(VillagePoolClassifier.isVillagePool("minecraft:village/plains/town_centers"), "vanilla village pool must match");
        check(VillagePoolClassifier.isVillagePool("villageworlds:village/jungle/start"), "modded village pool must match");
        check(!VillagePoolClassifier.isVillagePool("minecraft:bastion/starts"), "non-village pool must not match");

        check(ContentPoolClassifier.classify("villageworlds:village/jungle/start")
                == ContentPoolClassifier.ContentType.VILLAGE, "custom village pool classification");
        check(ContentPoolClassifier.classify("villageworlds:jungle_temple/start")
                == ContentPoolClassifier.ContentType.JUNGLE_TEMPLE, "temple pool classification");
        check(ContentPoolClassifier.classify("villageworlds:witch/start")
                == ContentPoolClassifier.ContentType.WITCH, "witch pool classification");
        check(ContentPoolClassifier.classify("villageworlds:exploration/watchtower")
                == ContentPoolClassifier.ContentType.EXPLORATION, "exploration pool classification");

        VillageWorldsConfig defaultsConfig = VillageWorldsConfig.defaults();
        check(defaultsConfig.terrainAwareVillages(), "terrain-aware villages must default on");
        check(defaultsConfig.terrainAwareStructures(), "terrain-aware structures must default on");
        check(defaultsConfig.newVillageTypes() && defaultsConfig.improvedJungleTemples()
                        && defaultsConfig.improvedWitchStructures() && defaultsConfig.additionalStructures(),
                "all content categories must default on");
        VillageWorldsConfig clamped = new VillageWorldsConfig(
                true, true, 999, 1, 100, 1, -1.0, 100.0,
                true, true, true, true
        ).validated();
        check(clamped.sampleRadius() == 96, "sample radius must clamp");
        check(clamped.sampleStep() == 4, "sample step must clamp");
        check(clamped.maxElevationRange() == 32, "elevation range must clamp");
        check(clamped.maxAdjacentDelta() == 2, "adjacent delta must clamp");
        check(clamped.minimumFlatRatio() == 0.25, "flat ratio must clamp");
        check(clamped.maximumRoughness() == 16.0, "roughness must clamp");

        ResourceSmokeTest.run();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
