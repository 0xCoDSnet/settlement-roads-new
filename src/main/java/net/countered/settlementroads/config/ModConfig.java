package net.countered.settlementroads.config;

import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.List;


public class ModConfig extends MidnightConfig {
    @Entry(category = "structures", name = "Number of structures to locate", isSlider = true, min = 0, max = 50)
    public static int rawNumberOfStructures = 10;

    @Entry(category = "structures", name = "Structure to Locate")
    public static String structureToLocate = "village";

    @Entry(category = "roads", name = "Distance between buoys")
    public static int distanceBetweenBuoys = 20;
}