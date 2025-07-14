package net.countered.settlementroads.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class ModConfig extends MidnightConfig {
    // "Maximum Number of structures to locate"
    @Entry(category = "structures")
    public static int maxLocatingCount = 100;

    // "Structure to locate"
    @Entry(category = "structures")
    public static String structureToLocate = "#minecraft:village";

    // "Number of structures to locate on world load"
    @Entry(category = "pre-generation")
    public static int initialLocatingCount = 7;

    // "Distance between buoys"
    @Entry(category = "roads")
    public static int distanceBetweenBuoys = 25;

    // "Artificial road averaging"
    @Entry(category = "roads")
    public static int averagingRadius = 1;

    // "Allow artificial roads"
    @Entry(category = "roads")
    public static boolean allowArtificial = true;

    // "Allow natural roads"
    @Entry(category = "roads")
    public static boolean allowNatural = true;

    // "Place waypoints instead of roads"
    @Entry(category = "roads")
    public static boolean placeWaypoints = false;

    // "Enable debug map"
    @Entry(category = "client")
    public static boolean enableDebugMap = false;

    // "Debug line color"
    @Entry(category = "client")
    public static int debugLineColor = 0xFFFFD000;

    // "Debug node color"
    @Entry(category = "client")
    public static int debugNodeColor = 0xFFAA00FF;
}