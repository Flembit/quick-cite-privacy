package dev.lukas.villageworlds.terrain;

public record TerrainReport(
        int minimumHeight,
        int maximumHeight,
        int elevationRange,
        int maximumAdjacentDelta,
        double flatRatio,
        double roughness,
        int samples
) {}
