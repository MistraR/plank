package com.mistra.plank.controller;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2022/2/22
 */

import com.mistra.plank.job.Barbarossa;
import com.mistra.plank.job.DailyRecordProcessor;
import com.mistra.plank.job.DragonListProcessor;
import com.mistra.plank.job.StockProcessor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlankController {

    private final Barbarossa barbarossa;
    private final StockProcessor stockProcessor;
    private final DragonListProcessor dragonListProcessor;
    private final DailyRecordProcessor dailyRecordProcessor;

    public PlankController(Barbarossa barbarossa, StockProcessor stockProcessor, DragonListProcessor dragonListProcessor,
                           DailyRecordProcessor dailyRecordProcessor) {
        this.barbarossa = barbarossa;
        this.stockProcessor = stockProcessor;
        this.dragonListProcessor = dragonListProcessor;
        this.dailyRecordProcessor = dailyRecordProcessor;
    }

    @PostMapping("/collectData")
    public void collectData() {
        dailyRecordProcessor.run(Barbarossa.STOCK_MAP);
    }

    @PostMapping("/replenish")
    public void replenish() {
        barbarossa.replenish();
    }

    @PostMapping("/monitor")
    public void monitor(String haveStock) {
        barbarossa.monitor(haveStock);
    }

    @PostMapping("/analyze")
    public void analyze() {
        barbarossa.analyze();
    }

    @PostMapping("/updateStock")
    public void updateStock() {
        stockProcessor.run();
    }

    @PostMapping("/updateDragonList")
    public void updateDragonList() {
        dragonListProcessor.run();
    }
}
