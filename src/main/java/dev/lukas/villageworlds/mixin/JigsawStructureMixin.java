package dev.lukas.villageworlds.mixin;

import dev.lukas.villageworlds.VillageWorldsMod;
import dev.lukas.villageworlds.config.VillageWorldsConfig;
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
    private void villageworlds$rejectUnsafeVillage(Structure.Context context,
                                                   CallbackInfoReturnable<Optional<Structure.StructurePosition>> cir) {
        VillageWorldsConfig config = VillageWorldsMod.config();
        if (!config.terrainAwareVillages()) return;

        String poolId = getStartPool().getKey()
                .map(key -> key.getValue().toString())
                .orElse("");
        if (!VillagePoolClassifier.isVillagePool(poolId)) return;

        int centerX = context.chunkPos().getStartX() + 8;
        int centerZ = context.chunkPos().getStartZ() + 8;
        TerrainReport report = TerrainAnalyzer.analyze(
                centerX,
                centerZ,
                config.sampleRadius(),
                config.sampleStep(),
                (x, z) -> context.chunkGenerator().getHeight(
                        x, z, Heightmap.Type.WORLD_SURFACE_WG, context.world(), context.noiseConfig())
        );
        TerrainThresholds thresholds = new TerrainThresholds(
                config.maxElevationRange(),
                config.maxAdjacentDelta(),
                config.minimumFlatRatio(),
                config.maximumRoughness()
        );

        if (!TerrainAnalyzer.isAcceptable(report, thresholds)) {
            VillageWorldsMod.LOGGER.debug(
                    "Rejected village pool {} at chunk {}: range={}, adjacentDelta={}, flatRatio={}, roughness={}",
                    poolId, context.chunkPos(), report.elevationRange(), report.maximumAdjacentDelta(),
                    report.flatRatio(), report.roughness());
            cir.setReturnValue(Optional.empty());
        }
    }
}
