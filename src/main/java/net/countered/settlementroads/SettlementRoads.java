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
	// IMPORTANT
	// Bridges
	// Tunnels
	// Lanterns
	// Distance signs (being removed because post processing)
	// Make artificial roads more artificial
	// Remove placed blocks from caches
	// Possibly incorrect roads / broken roads when recaching on world reload? fix: first cache roads generated on world load, then cache additional ones 1 by 1. Prerequisite: village locations need to be saved unordered persistent
	// Add mixed stone roads / mud / dirt
	// fix roads on snow icebergs

	// OPTIONAL
	// Road qualities blocks: mossy variants, rooted dirt,
	// Biome specific road changes
	// Location lag reducing (async locator?)

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