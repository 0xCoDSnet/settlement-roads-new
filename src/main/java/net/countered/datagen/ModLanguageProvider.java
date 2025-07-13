package net.countered.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

import java.util.HashMap;
import java.util.Map;

public class ModLanguageProvider {
    public static class EnUsProvider extends FabricLanguageProvider {
        public static final Map<String, String> TRANSLATIONS = new HashMap<>();

        static {
            TRANSLATIONS.put("settlement-roads.midnightconfig.category.structures", "Structures");
            TRANSLATIONS.put("settlement-roads.midnightconfig.category.roads", "Roads");
            TRANSLATIONS.put("settlement-roads.midnightconfig.category.pre-generation", "Pre-Generation");
            TRANSLATIONS.put("settlement-roads.midnightconfig.initialLocatingCount", "Number of structures to locate on world load");
            TRANSLATIONS.put("settlement-roads.midnightconfig.initialLocatingCount.tooltip", "Select the number of structures to locate on world load. Higher number = longer loading time");
            TRANSLATIONS.put("settlement-roads.midnightconfig.maxLocatingCount", "Maximum number of structures to locate");
            TRANSLATIONS.put("settlement-roads.midnightconfig.maxLocatingCount.tooltip", "Select the maximum number of structures to locate dynamically. Higher number = more memory usage");
            TRANSLATIONS.put("settlement-roads.midnightconfig.structureToLocate", "Structure to locate");
            TRANSLATIONS.put("settlement-roads.midnightconfig.structureToLocate.tooltip", "Select or type the structure you want to locate. E.g., #minecraft:village, minecraft:village_plains or modid:custom_structure. Allows structure tags and keys.");
            TRANSLATIONS.put("settlement-roads.midnightconfig.distanceBetweenBuoys", "Distance between buoys");
            TRANSLATIONS.put("settlement-roads.midnightconfig.distanceBetweenBuoys.tooltip", "Approximate distance between buoys in blocks");
            TRANSLATIONS.put("settlement-roads.midnightconfig.averagingRadius", "Averaging radius");
            TRANSLATIONS.put("settlement-roads.midnightconfig.averagingRadius.tooltip", "Amount of terrain averaging done for artificial roads");
            TRANSLATIONS.put("settlement-roads.midnightconfig.allowArtificial", "Allow artificial roads");
            TRANSLATIONS.put("settlement-roads.midnightconfig.allowArtificial.tooltip", "Whether artificial roads are allowed to generate");
            TRANSLATIONS.put("settlement-roads.midnightconfig.allowNatural", "Allow natural roads");
            TRANSLATIONS.put("settlement-roads.midnightconfig.allowNatural.tooltip", "Whether natural roads are allowed to generate");
            TRANSLATIONS.put("settlement-roads.midnightconfig.placeWaypoints", "Place Waypoints instead of Roads");
            TRANSLATIONS.put("settlement-roads.midnightconfig.placeWaypoints.tooltip", "Disables road generation in favor of waypoint generation");
            TRANSLATIONS.put("settlement-roads.midnightconfig.title", "Countered's Settlement Roads");
        }

        public EnUsProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
            super(output, "en_us", registryLookup);
        }

        @Override
        public void generateTranslations(RegistryWrapper.WrapperLookup registryLookup, TranslationBuilder builder) {
            TRANSLATIONS.forEach(builder::add);
        }
    }

    public static class RuRuProvider extends FabricLanguageProvider {
        public RuRuProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
            super(output, "ru_ru", registryLookup);
        }

        @Override
        public void generateTranslations(RegistryWrapper.WrapperLookup registryLookup, TranslationBuilder builder) {
            EnUsProvider.TRANSLATIONS.forEach((key, value) ->
                    builder.add(key, RUSSIAN_TRANSLATIONS.getOrDefault(key, value)));
        }

        private static final Map<String, String> RUSSIAN_TRANSLATIONS = new HashMap<>();

        static {
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.category.structures", "Структуры");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.category.roads", "Дороги");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.category.pre-generation", "Предварительная генерация");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.initialLocatingCount", "Сколько структур искать при загрузке мира");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.initialLocatingCount.tooltip", "Выберите количество структур для поиска при загрузке. Больше значение = дольше загрузка");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.maxLocatingCount", "Максимальное количество структур для поиска");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.maxLocatingCount.tooltip", "Выберите максимум структур, для динамического поиска. Больше значение = больше памяти");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.structureToLocate", "Структура для поиска");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.structureToLocate.tooltip", "Выберите или введите нужную структуру. #minecraft:village, minecraft:village_plains или modid:custom_structure. Допускаются теги и ключи.");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.distanceBetweenBuoys", "Расстояние между буями");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.distanceBetweenBuoys.tooltip", "Примерное расстояние между буями в блоках");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.averagingRadius", "Радиус усреднения");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.averagingRadius.tooltip", "Объем сглаживания рельефа для искусственных дорог");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.allowArtificial", "Разрешить искусственные дороги");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.allowArtificial.tooltip", "Генерируются ли искусственные дороги");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.allowNatural", "Разрешить природные дороги");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.allowNatural.tooltip", "Генерируются ли природные дороги");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.placeWaypoints", "Размещать маяки вместо дорог");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.placeWaypoints.tooltip", "Отключает генерацию дорог в пользу маяков");
            RUSSIAN_TRANSLATIONS.put("settlement-roads.midnightconfig.title", "Дороги поселений от Countered");
        }
    }
}