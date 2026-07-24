package dev.lukas.villageworlds.terrain;

public final class PureSmokeTest {
    public static void main(String[] args) {
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
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
