package dev.lukas.villageworlds.mixin;

import dev.lukas.villageworlds.VillageWorldsMod;
import dev.lukas.villageworlds.config.VillageWorldsConfig;
import dev.lukas.villageworlds.terrain.ContentPoolClassifier;
import dev.lukas.villageworlds.terrain.TerrainAnalyzer;
import dev.lukas.villageworlds.terrain.TerrainReport;
import dev.lukas.villageworlds.terrain.TerrainThresholds;
import dev.lukas.villageworlds.terrain.VillagePoolClassifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.JigsawStructure;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(JigsawStructure.class)
public abstract class JigsawStructureMixin {
    @Shadow
    public abstract RegistryEntry<StructurePool> getStartPool();

    @Inject(method = "getStructurePosition", at = @At("HEAD"), cancellable = true)
    private void villageworlds$rejectUnsafeStructure(Structure.Context context,
                                                     CallbackInfoReturnable<Optional<Structure.StructurePosition>> cir) {
        VillageWorldsConfig config = VillageWorldsMod.config();
        String poolId = getStartPool().getKey()
                .map(key -> key.getValue().toString())
                .orElse("");

        ContentPoolClassifier.ContentType contentType = ContentPoolClassifier.classify(poolId);
        if (!ContentPoolClassifier.enabled(contentType, config)) {
            cir.setReturnValue(Optional.empty());
            return;
        }

        boolean village = VillagePoolClassifier.isVillagePool(poolId);
        boolean customStructure = contentType != ContentPoolClassifier.ContentType.NONE;
        if ((!village || !config.terrainAwareVillages())
                && (!customStructure || !config.terrainAwareStructures())) {
            return;
        }

        TerrainRule rule = ruleFor(contentType, village, config);
        int centerX = context.chunkPos().getStartX() + 8;
        int centerZ = context.chunkPos().getStartZ() + 8;
        TerrainReport report = TerrainAnalyzer.analyze(
                centerX,
                centerZ,
                rule.radius(),
                rule.step(),
                (x, z) -> context.chunkGenerator().getHeight(
                        x, z, Heightmap.Type.WORLD_SURFACE_WG, context.world(), context.noiseConfig())
        );

        if (!TerrainAnalyzer.isAcceptable(report, rule.thresholds())) {
            VillageWorldsMod.LOGGER.debug(
                    "Rejected structure pool {} at chunk {}: range={}, adjacentDelta={}, flatRatio={}, roughness={}",
                    poolId, context.chunkPos(), report.elevationRange(), report.maximumAdjacentDelta(),
                    report.flatRatio(), report.roughness());
            cir.setReturnValue(Optional.empty());
        }
    }

    private static TerrainRule ruleFor(ContentPoolClassifier.ContentType type,
                                       boolean village,
                                       VillageWorldsConfig config) {
        if (village) {
            return new TerrainRule(
                    config.sampleRadius(),
                    config.sampleStep(),
                    new TerrainThresholds(
                            config.maxElevationRange(),
                            config.maxAdjacentDelta(),
                            config.minimumFlatRatio(),
                            config.maximumRoughness()
                    )
            );
        }
        return switch (type) {
            case JUNGLE_TEMPLE -> new TerrainRule(18, 6, new TerrainThresholds(20, 8, 0.42, 7.0));
            case WITCH -> new TerrainRule(16, 4, new TerrainThresholds(14, 6, 0.52, 5.5));
            case EXPLORATION -> new TerrainRule(18, 6, new TerrainThresholds(18, 7, 0.45, 6.5));
            case NONE, VILLAGE -> new TerrainRule(16, 8, new TerrainThresholds(20, 8, 0.40, 7.5));
        };
    }

    private record TerrainRule(int radius, int step, TerrainThresholds thresholds) {}
}
