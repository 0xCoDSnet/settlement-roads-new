package net.countered.settlementroads.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class ModConfig extends MidnightConfig {
    @Entry(category = "structures", name = "Number of structures to locate", isSlider = true, min = 0, max = 50)
    public static int rawNumberOfStructures = 10;

    @Entry(category = "structures", name = "Structure to locate")
    public static String structureToLocate = "village";

    @Entry(category = "roads", name = "Distance between buoys")
    public static int distanceBetweenBuoys = 20;

    @Entry(category = "roads", name = "Allow artificial roads")
    public static boolean allowArtificial = true;

    @Entry(category = "roads", name = "Allow natural roads")
    public static boolean allowNatural = true;
}