package com.mistra.plank.controller;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mistra.plank.common.util.StringUtil;
import com.mistra.plank.job.Barbarossa;
import com.mistra.plank.job.DailyRecordProcessor;
import com.mistra.plank.job.StockProcessor;
import com.mistra.plank.model.entity.Stock;
import com.mistra.plank.model.param.AutoTradeParam;
import com.mistra.plank.model.param.FundHoldingsParam;
import com.mistra.plank.model.param.SelfSelectParam;
import com.mistra.plank.service.StockSelectedService;

/**
 * @author mistra@future.com
 * @date 2021/11/19
 */
@RestController
public class PlankController {

    private final StockProcessor stockProcessor;
    private final StockSelectedService stockSelectedService;
    private final DailyRecordProcessor dailyRecordProcessor;

    public PlankController(StockProcessor stockProcessor, StockSelectedService stockSelectedService, DailyRecordProcessor dailyRecordProcessor) {
        this.stockProcessor = stockProcessor;
        this.stockSelectedService = stockSelectedService;
        this.dailyRecordProcessor = dailyRecordProcessor;
    }

    /**
     * 导入基金季度持仓数据
     * 基金季度持仓数据导出步骤：
     * Choice金融客户端：基金->资产配置->重仓持股(汇总)->选择某个季度->提取数据->导出到Excel
     *
     * @param fundHoldingsParam FundHoldingsParam
     * @param beginTime         季度开始时间
     * @param endTime           季度结束时间
     */
    @PostMapping("/fund-holdings/{beginTime}/{endTime}")
    public void fundHoldingsImport(FundHoldingsParam fundHoldingsParam,
                                   @PathVariable(value = "beginTime") Long beginTime, @PathVariable(value = "endTime") Long endTime) {
        stockProcessor.fundHoldingsImport(fundHoldingsParam, new Date(beginTime), new Date(endTime));
    }

    /**
     * 编辑web页面自选
     *
     * @param selfSelectParam SelfSelectParam
     */
    @PostMapping("/add-self-select")
    public void addSelfSelect(@RequestBody SelfSelectParam selfSelectParam) {
        stockSelectedService.addSelfSelect(selfSelectParam);
    }

    /**
     * 编辑自动打板股票
     *
     * @param selfSelectParam SelfSelectParam
     */
    @PostMapping("/add-auto-plank")
    public void addAutoPlank(@RequestBody SelfSelectParam selfSelectParam) {
        stockSelectedService.addAutoPlank(selfSelectParam);
    }

    /**
     * 查询自动打板盯盘的股票
     */
    @GetMapping("/get-auto-plank")
    public String getAutoPlank() {
        List<String> list = Barbarossa.SZ30_STOCK_MAP.values().stream().map(Stock::getName).collect(Collectors.toList());
        list.addAll(Barbarossa.SH10_STOCK_MAP.values().stream().map(Stock::getName).collect(Collectors.toList()));
        return StringUtil.collectionToString(list);
    }

    /**
     * 下一个交易日自动交易池
     *
     * @param autoTradeParams autoTradeParams
     */
    @PostMapping("/tomorrow-auto-trade-pool")
    public void tomorrowAutoTradePool(@RequestBody List<AutoTradeParam> autoTradeParams) {
        stockSelectedService.tomorrowAutoTradePool(autoTradeParams);
    }

    /**
     * 更新某支股票最近recentDayNumber天的交易数据
     */
    @PostMapping("/update-dailyRecord-byCode")
    public void updateByName(@RequestParam String name, @RequestParam Integer recentDayNumber) {
        dailyRecordProcessor.updateByName(name, recentDayNumber);
    }

}
