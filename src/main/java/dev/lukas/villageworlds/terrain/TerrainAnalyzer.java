package dev.lukas.villageworlds.terrain;

public final class TerrainAnalyzer {
    private TerrainAnalyzer() {}

    public static TerrainReport analyze(int centerX, int centerZ, int radius, int step, HeightSampler sampler) {
        int width = (radius * 2 / step) + 1;
        int[][] heights = new int[width][width];
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int sum = 0;
        int count = 0;

        for (int ix = 0; ix < width; ix++) {
            int x = centerX - radius + ix * step;
            for (int iz = 0; iz < width; iz++) {
                int z = centerZ - radius + iz * step;
                int height = sampler.sample(x, z);
                heights[ix][iz] = height;
                min = Math.min(min, height);
                max = Math.max(max, height);
                sum += height;
                count++;
            }
        }

        double mean = (double) sum / count;
        double squaredError = 0.0;
        int flatSamples = 0;
        int maxDelta = 0;
        for (int ix = 0; ix < width; ix++) {
            for (int iz = 0; iz < width; iz++) {
                int height = heights[ix][iz];
                squaredError += Math.pow(height - mean, 2);
                if (Math.abs(height - mean) <= 3.0) flatSamples++;
                if (ix + 1 < width) maxDelta = Math.max(maxDelta, Math.abs(height - heights[ix + 1][iz]));
                if (iz + 1 < width) maxDelta = Math.max(maxDelta, Math.abs(height - heights[ix][iz + 1]));
            }
        }

        return new TerrainReport(min, max, max - min, maxDelta,
                (double) flatSamples / count, Math.sqrt(squaredError / count), count);
    }

    public static boolean isAcceptable(TerrainReport report, TerrainThresholds thresholds) {
        return report.elevationRange() <= thresholds.maxElevationRange()
                && report.maximumAdjacentDelta() <= thresholds.maxAdjacentDelta()
                && report.flatRatio() >= thresholds.minimumFlatRatio()
                && report.roughness() <= thresholds.maximumRoughness();
    }
}
