package net.countered.settlementroads.villagelocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static net.countered.settlementroads.SettlementRoads.MOD_ID;

public class VillageLocator {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicInteger villagesToLocate;
    private final AtomicInteger locatedVillages = new AtomicInteger(0);

    public VillageLocator(int numberOfVillages) {
        this.villagesToLocate = new AtomicInteger(numberOfVillages);
    }

    public void locateVillagesAsync(Runnable locateVillagesTask) {
        executor.submit(() -> {
            locateVillagesTask.run();
            if (locatedVillages.incrementAndGet() >= villagesToLocate.get()) {
                shutdown();  // Shutdown after locating all villages
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
        LOGGER.info("Shutting down village locator");
    }
}
