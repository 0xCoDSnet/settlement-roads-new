package net.countered.settlementroads.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class ModConfig extends MidnightConfig {
    @Entry(category = "structures", name = "Number of structures to locate on world load", isSlider = true, min = 0, max = 50)
    public static int initialLocatingCount = 5;

    @Entry(category = "structures", name = "Maximum Number of structures to locate", isSlider = true, min = 5, max = 1000)
    public static int maxLocatingCount = 50;

    @Entry(category = "structures", name = "Structure to locate")
    public static String structureToLocate = "village";

    @Entry(category = "roads", name = "Distance between buoys")
    public static int distanceBetweenBuoys = 25;

    @Entry(category = "roads", name = "Allow artificial roads")
    public static boolean allowArtificial = true;

    @Entry(category = "roads", name = "Allow natural roads")
    public static boolean allowNatural = true;

    @Entry(category = "roads", name = "Place waypoints instead")
    public static boolean placeWaypoints = false;
}