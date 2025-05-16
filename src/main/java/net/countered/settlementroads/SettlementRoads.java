package net.countered.settlementroads;

import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.events.ModEventHandler;
import net.countered.settlementroads.features.config.RoadFeatureRegistry;
import net.countered.settlementroads.persistence.attachments.WorldDataAttachment;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettlementRoads implements ModInitializer {

	public static final String MOD_ID = "settlement-roads";

	private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

	// TODO
	// in  feature add new village connection + cache
	// Check cache in servertickevents scan world chunks, generate road, add to world
	// in feature read road data and place


	// Fix:
	// Caching should not change already established connections (broken roads)
	// Remove config for loading chunks
	// Clean snow from roads
	// fix locating in other dimensions

	// Change:
	// Artificial roads look bad
	// Make caching save placement locations in chunk attachments
	// Place road blocks per chunk via attachment
	// Make road type return enum

	// Add:
	//

	// OPTIONAL
	// Possibly broken roads when recaching on world reload?
	// Biome specific road changes
	// Location lag reducing (async locator?)/ structure essentials / place instant roads?
	// Bridges
	// Tunnels
	// fix roads on snow icebergs
	// Remove placed blocks from caches
	// place slabs on artificial roads

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Settlement Roads...");
		WorldDataAttachment.registerWorldDataAttachment();
		ModConfig.init(MOD_ID, ModConfig.class);
		RoadFeatureRegistry.registerFeatures();
		ModEventHandler.register();
	}
}