package com.mistra.plank.job;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.date.DateUtil;
import com.google.common.collect.Lists;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.model.dto.StockRealTimePrice;
import com.mistra.plank.model.enums.AutomaticTradingEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * @author rui.wang
 * @ Version: 1.0
 * @ Time: 2023/2/15 13:17
 * @ Description: 自动打板交易，开启的话会监控成交额大于3亿的全市场股票，发现上板则会自动下单排队
 * 只打首板，反包板。即昨日没有上板的股票
 * 昨日连板的股票我想的是自己过滤之后再选择打哪个，就通过AutomaticTrading提前配置来打板
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "plank", name = "automaticPlankTrading", havingValue = "true")
public class AutomaticPlankTrading implements CommandLineRunner {

    private final PlankConfig plankConfig;
    private final StockProcessor stockProcessor;

    private final AutomaticTrading automaticTrading;
    /**
     * 涨幅>7个点的股票，50毫秒查询一次数据，上板则下单排队
     */
    private static final ConcurrentHashSet<String> INCR_GE_7 = new ConcurrentHashSet<>();

    public AutomaticPlankTrading(PlankConfig plankConfig, StockProcessor stockProcessor, AutomaticTrading automaticTrading) {
        this.plankConfig = plankConfig;
        this.stockProcessor = stockProcessor;
        this.automaticTrading = automaticTrading;
    }

    /**
     * 每30秒过滤涨幅大于7个点股票，放入INCR_GE_7
     */
    @Scheduled(cron = "*/30 * * * * ?")
    private void filterIncrGe7() {
        if (AutomaticTrading.isTradeTime() && AutomaticTrading.todayCostMoney < plankConfig.getAutomaticTradingMoney()) {
            List<List<String>> lists = Lists.partition(Lists.newArrayList(Barbarossa.STOCK_MAP_GE_3E.keySet()),
                    Barbarossa.executorService.getMaximumPoolSize());
            for (List<String> list : lists) {
                Barbarossa.executorService.submit(() -> filterIncrGe7(list));
            }
        }
    }

    /**
     * 过滤涨幅大于7个点股票
     *
     * @param codes codes
     */
    private void filterIncrGe7(List<String> codes) {
        codes.forEach(e -> {
            StockRealTimePrice stockRealTimePriceByCode = stockProcessor.getStockRealTimePriceByCode(e);
            if (stockRealTimePriceByCode.getIncreaseRate() > 7) {
                INCR_GE_7.add(e);
            }
        });
    }

    /**
     * 一直监控涨幅大于7个点股票
     */
    @Override
    public void run(String... args) throws Exception {
        filterIncrGe7();
        Barbarossa.executorService.submit(this::autoPlank);
    }

    /**
     * 只打上午的板 DateUtil.isAM(new Date());
     * 或者10点以前涨停的板
     */
    private void autoPlank() {
        Date date = new Date();
        while (DateUtil.hour(date, true) < 10 && AutomaticTrading.todayCostMoney < plankConfig.getAutomaticTradingMoney()) {
            if (AutomaticTrading.isTradeTime() && !INCR_GE_7.isEmpty()) {
                for (String code : INCR_GE_7) {
                    StockRealTimePrice stockRealTimePriceByCode = stockProcessor.getStockRealTimePriceByCode(code);
                    if (stockRealTimePriceByCode.isPlank() &&
                            stockRealTimePriceByCode.getCurrentPrice() * 100 < plankConfig.getSingleTransactionLimitAmount()) {
                        // 上板，下单排队
                        int sum = 0;
                        int amount = 0;
                        for (amount = 100; sum <= plankConfig.getSingleTransactionLimitAmount(); amount = amount + 100) {
                            sum = (int) (amount * stockRealTimePriceByCode.getCurrentPrice());
                        }
                        automaticTrading.buy(Barbarossa.STOCK_MAP_GE_3E.get(code), amount, stockRealTimePriceByCode.getLimitUp(),
                                AutomaticTradingEnum.AUTO_PLANK);
                        Barbarossa.STOCK_MAP_GE_3E.remove(code);
                        INCR_GE_7.remove(code);
                    } else if (stockRealTimePriceByCode.getIncreaseRate() < 5) {
                        INCR_GE_7.remove(code);
                    }
                }
            } else {
                try {
                    Thread.sleep(30 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
