package net.countered.settlementroads.helpers;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.events.ModEventHandler;
import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.persistence.RoadData;
import net.fabricmc.fabric.mixin.registry.sync.RegistriesAccessor;
import net.minecraft.command.argument.RegistryPredicateArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Helpers {

    public static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

    public static void locateStructures(ServerWorld serverWorld, String structureId, int count, Boolean isTag) throws CommandSyntaxException {
        LOGGER.info("Locating structure: " + structureId);
        LOGGER.info("Locating " + count + " structures...");
        if (isTag) {
            TagKey<Structure> structureTag = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of(structureId));

            for (int x = 0; x < count; x++) {
                BlockPos structureLocation = serverWorld.locateStructure(structureTag, serverWorld.getSpawnPos(), 100, true);

                if (structureLocation != null) {
                    LOGGER.info("Structure found at " + structureLocation);
                    RoadData.getOrCreateRoadData(serverWorld).getStructureLocations().add(structureLocation);
                }
            }
        }
        else {
            StringReader reader = new StringReader(structureId);

            RegistryKey<Registry<Structure>> structureRegistryKey = RegistryKeys.STRUCTURE;
            RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate =
                    new RegistryPredicateArgumentType<>(structureRegistryKey).parse(reader);

            Registry<Structure> registry = serverWorld.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);

            RegistryEntryList<Structure> registryEntryList = (RegistryEntryList<Structure>) getStructureListForPredicate(predicate, registry)
                    .orElseThrow(() -> new IllegalArgumentException("Structure not found for identifier: " + structureId));

            for (int x = 0; x < count; x++) {
                Pair<BlockPos, RegistryEntry<Structure>> structureLocation = serverWorld.getChunkManager()
                        .getChunkGenerator()
                        .locateStructure(serverWorld, registryEntryList, serverWorld.getSpawnPos(), 100, true);

                if (structureLocation != null) {
                    LOGGER.info("Structure found at " + structureLocation);
                    RoadData.getOrCreateRoadData(serverWorld).getStructureLocations().add(structureLocation.getFirst());
                }
            }
        }
    }

    private static Optional<? extends RegistryEntryList.ListBacked<Structure>> getStructureListForPredicate(
            RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate, Registry<Structure> structureRegistry
    ) {
        return predicate.getKey().map(key -> structureRegistry.getOptional(key).map(entry -> RegistryEntryList.of(entry)), structureRegistry::getOptional);
    }
}
