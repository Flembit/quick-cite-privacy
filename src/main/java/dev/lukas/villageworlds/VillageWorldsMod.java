package dev.lukas.villageworlds;

import dev.lukas.villageworlds.config.VillageWorldsConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VillageWorldsMod implements ModInitializer {
    public static final String MOD_ID = "villageworlds";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static volatile VillageWorldsConfig config = VillageWorldsConfig.defaults();

    @Override
    public void onInitialize() {
        config = VillageWorldsConfig.load();
        LOGGER.info("Village Worlds initialized: radius={}, step={}, maxRange={}, maxAdjacentDelta={}",
                config.sampleRadius(), config.sampleStep(), config.maxElevationRange(), config.maxAdjacentDelta());
    }

    public static VillageWorldsConfig config() {
        return config;
    }
}
