package com.mistra.plank.controller;

import java.util.Date;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mistra.plank.job.Barbarossa;
import com.mistra.plank.job.DailyRecordProcessor;
import com.mistra.plank.job.DragonListProcessor;
import com.mistra.plank.job.StockProcessor;
import com.mistra.plank.pojo.param.FundHoldingsParam;

/**
 * @author mistra@future.com
 * @date 2021/11/19
 */
@RestController
public class PlankController {

    private final Barbarossa barbarossa;
    private final StockProcessor stockProcessor;
    private final DragonListProcessor dragonListProcessor;
    private final DailyRecordProcessor dailyRecordProcessor;

    public PlankController(Barbarossa barbarossa, StockProcessor stockProcessor,
        DragonListProcessor dragonListProcessor, DailyRecordProcessor dailyRecordProcessor) {
        this.barbarossa = barbarossa;
        this.stockProcessor = stockProcessor;
        this.dragonListProcessor = dragonListProcessor;
        this.dailyRecordProcessor = dailyRecordProcessor;
    }

    /**
     * 抓取近日股票涨跌数据
     */
    @PostMapping("/collectData")
    public void collectData() {
        dailyRecordProcessor.run(Barbarossa.STOCK_MAP);
    }

    /**
     * 分析连板晋级率
     */
    @PostMapping("/analyze")
    public void analyze() {
        barbarossa.analyze();
    }

    /**
     * 更新新上市的股票数据
     */
    @PostMapping("/updateStock")
    public void updateStock() {
        stockProcessor.run();
    }

    /**
     * 更新龙虎榜数据
     */
    @PostMapping("/updateDragonList")
    public void updateDragonList() {
        dragonListProcessor.run();
    }

    /**
     * 以历史数据为样本，根据配置的买入，卖出，分仓策略自动交易 确保龙虎榜数据已经更新到最新日期
     */
    @PostMapping("/barbarossa")
    public void barbarossa() {
        barbarossa.barbarossa();
    }

    /**
     * 导入基金季度持仓数据
     */
    @PostMapping("/fund-holdings/{beginTime}/{endTime}")
    public void fundHoldingsImport(FundHoldingsParam fundHoldingsParam,
        @PathVariable(value = "beginTime") Long beginTime, @PathVariable(value = "endTime") Long endTime) {
        barbarossa.fundHoldingsImport(fundHoldingsParam, new Date(beginTime), new Date(endTime));
    }
}
