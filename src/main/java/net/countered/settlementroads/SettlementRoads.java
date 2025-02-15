package net.countered.settlementroads;

import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.events.ModEventHandler;
import net.countered.settlementroads.features.RoadFeature;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettlementRoads implements ModInitializer {

	public static final String MOD_ID = "settlement-roads";

	public static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

	// TODO
	// Biome specific road changes
	// Location lag reducing
	// Mile Markers
	// Bridges
	// Tunnels
	// Lanterns
	// Benches
	// Distance signs
	// Road qualities blocks: mossy variants, rooted dirt,
	// config to place landmarks instead of roads
	// clear snow from road?
	// Make artificial roads more artificial
	// Remove placed blocks from caches
	// Possibly incorrect roads / broken roads when recaching on world reload?

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Settlement Roads...");
		ModConfig.init(MOD_ID, ModConfig.class);
		registerFeatures();
		ModEventHandler.register();
	}

	private void registerFeatures() {
		LOGGER.info("Registering Features...");
		Registry.register(Registries.FEATURE, Identifier.of(MOD_ID, "road_feature"), RoadFeature.ROAD_FEATURE);
		BiomeModifications.addFeature(
				BiomeSelectors.all(),
				GenerationStep.Feature.UNDERGROUND_STRUCTURES,
				RoadFeature.ROAD_FEATURE_PLACED_KEY
		);
	}
}