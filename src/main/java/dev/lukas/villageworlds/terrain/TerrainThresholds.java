package dev.lukas.villageworlds.terrain;

public record TerrainThresholds(
        int maxElevationRange,
        int maxAdjacentDelta,
        double minimumFlatRatio,
        double maximumRoughness
) {}
