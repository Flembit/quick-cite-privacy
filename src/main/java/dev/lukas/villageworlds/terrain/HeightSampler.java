package dev.lukas.villageworlds.terrain;

@FunctionalInterface
public interface HeightSampler {
    int sample(int x, int z);
}
