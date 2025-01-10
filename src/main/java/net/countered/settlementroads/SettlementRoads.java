package net.countered.settlementroads;

import net.countered.settlementroads.events.ModEventHandler;
import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.features.RoadFeatureConfig;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.SquarePlacementModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SettlementRoads implements ModInitializer {

	public static final String MOD_ID = "settlement-roads";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		LOGGER.info("Initializing Settlement Roads...");
		registerFeatures();
		ModEventHandler.register();
	}

	private void registerFeatures() {
		LOGGER.info("Registering Features...");
		Registry.register(Registries.FEATURE, Identifier.of(MOD_ID, "road_feature"), RoadFeature.ROAD_FEATURE);
		BiomeModifications.addFeature(
				BiomeSelectors.all(),
				GenerationStep.Feature.RAW_GENERATION,
				RoadFeature.ROAD_FEATURE_PLACED_KEY
		);
	}
}