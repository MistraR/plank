package com.mistra.plank.job;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.dao.HoldSharesMapper;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.model.dto.StockRealTimePrice;
import com.mistra.plank.model.entity.HoldShares;
import com.mistra.plank.model.entity.Stock;
import com.mistra.plank.model.enums.AutomaticTradingEnum;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Mistra @ Version: 1.0
 * @ Time: 2021/11/18 22:09
 * @ Copyright (c) Mistra,All Rights Reserved
 * @ Github: https://github.com/MistraR
 * @ CSDN: https://blog.csdn.net/axela30w
 * @ Description: 根据不同策略筛选出来的股票 新开一个线程 plank(Stock stock); 发现上板则会自动下单排队 目前有：
 * 1.20cm首板 - 隔日溢价率挺高的 filterStock()
 * 2.10cm 开盘30分钟内上板的     filterStock()
 * 3.自己复盘筛选的打板标的 - 筛选出人气龙头 selectAutoPlankStock()
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "plank", name = "automaticPlankTrading", havingValue = "true")
public class AutomaticPlankTrading implements CommandLineRunner {

    private static final int availableProcessors = Runtime.getRuntime().availableProcessors();
    private final PlankConfig plankConfig;
    private final StockProcessor stockProcessor;
    private final AutomaticTrading automaticTrading;
    private final StockMapper stockMapper;
    private final HoldSharesMapper holdSharesMapper;

    /**
     * 正在盯板中的股票
     * 创业板涨幅>17或主板涨幅>7.5的股票会新起一个线程盯板,低于这个涨幅的会暂时取消掉盯板
     */
    public static final ConcurrentHashMap<String, Stock> PLANKING_CACHE = new ConcurrentHashMap<>();

    /**
     * 打板一级缓存,创业板涨幅>10或主板涨幅>5的股票,低于这个涨幅的会被移除
     */
    public static final ConcurrentHashMap<String, Stock> PLANK_LEVEL1_CACHE = new ConcurrentHashMap<>();

    public static final ThreadPoolExecutor PLANK_POOL = new ThreadPoolExecutor(availableProcessors * 3, availableProcessors * 3, 100L,
            TimeUnit.SECONDS, new SynchronousQueue<>(), new NamedThreadFactory("Plank-", false));

    public AutomaticPlankTrading(PlankConfig plankConfig, StockProcessor stockProcessor, AutomaticTrading automaticTrading, StockMapper stockMapper
            , HoldSharesMapper holdSharesMapper) {
        this.plankConfig = plankConfig;
        this.stockProcessor = stockProcessor;
        this.automaticTrading = automaticTrading;
        this.stockMapper = stockMapper;
        this.holdSharesMapper = holdSharesMapper;
    }

    @Override
    public void run(String... args) {
        selectAutoPlankStock();
    }

    @Scheduled(cron = "*/3 * * * * ?")
    private void filterStock() {
        if (openAutoPlank()) {
            Set<String> set = selectTodayTradedStock();
            PLANK_LEVEL1_CACHE.keySet().stream().filter(e -> !set.contains(e) && !PLANKING_CACHE.containsKey(e)).collect(Collectors.toList()).parallelStream().forEach(this::filterStock);
            selectAutoPlankStock();
        }
    }

    private void filterStock(String e) {
        try {
            StockRealTimePrice stockRealTimePriceByCode = stockProcessor.getStockRealTimePriceByCode(e);
            if (e.startsWith("SZ30")) {
                if (stockRealTimePriceByCode.getIncreaseRate() > 17) {
                    this.plank(PLANK_LEVEL1_CACHE.get(e));
                } else if (stockRealTimePriceByCode.getIncreaseRate() < 10) {
                    PLANK_LEVEL1_CACHE.remove(e);
                }
            } else {
                if (stockRealTimePriceByCode.getIncreaseRate() > 7.5) {
                    this.plank(PLANK_LEVEL1_CACHE.get(e));
                } else if (stockRealTimePriceByCode.getIncreaseRate() < 5.5) {
                    PLANK_LEVEL1_CACHE.remove(e);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * 获取今日交易过(买入,卖出)的股票code,今日交易过的票就不再买入了
     *
     * @return Set<String> todayBufSaleSet = selectTodayBufSaleSet();
     */
    public Set<String> selectTodayTradedStock() {
        List<HoldShares> holdShares = holdSharesMapper.selectList(new LambdaQueryWrapper<HoldShares>().ge(HoldShares::getSaleTime,
                DateUtil.beginOfDay(new Date())).or().ge(HoldShares::getBuyTime, DateUtil.beginOfDay(new Date())));
        return holdShares.stream().map(HoldShares::getCode).collect(Collectors.toSet());
    }

    /**
     * 查询需要自动打板的股票
     */
    public void selectAutoPlankStock() {
        List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>().eq(Stock::getAutomaticTradingType,
                AutomaticTradingEnum.AUTO_PLANK.name()));
        Set<String> todayBufSaleSet = selectTodayTradedStock();
        for (Stock stock : stocks) {
            if (!todayBufSaleSet.contains(stock.getCode()) && !PLANKING_CACHE.containsKey(stock.getCode())) {
                this.plank(stock);
            }
        }
    }

    public void plank(Stock stock) {
        PLANK_POOL.submit(new AutoPlankTask(stock));
    }

    /**
     * 每个打板标的一个线程，尽最快速度获取实时价格与挂单 打板标的数量不宜过多，系统其他获取数据的线程也会消耗CPU资源
     */
    class AutoPlankTask implements Runnable {

        private final Stock stock;

        public AutoPlankTask(Stock stock) {
            this.stock = stock;
        }

        @Override
        public void run() {
            if (PLANKING_CACHE.containsKey(stock.getCode())) {
                return;
            }
            if (PLANKING_CACHE.size() >= availableProcessors * 3) {
                log.warn("打板线程过多,取消盯板 {}", stock.getName());
            }
            log.warn("{} 新加入打板监测", stock.getName());
            boolean watch = true;
            while (watch) {
                PLANKING_CACHE.put(stock.getCode(), stock);
                try {
                    if (openAutoPlank()) {
                        StockRealTimePrice stockRealTimePriceByCode = stockProcessor.getStockRealTimePriceByCode(stock.getCode());
                        double v = stockRealTimePriceByCode.getCurrentPrice() * 100;
                        if (AutomaticTrading.TODAY_COST_MONEY.intValue() + v > plankConfig.getAutomaticTradingMoneyLimitUp()) {
                            log.warn("今日自动买入金额已接近上限:{},取消打板监控:{}", AutomaticTrading.TODAY_COST_MONEY.intValue(), stock.getName());
                            PLANKING_CACHE.remove(stock.getCode());
                            watch = false;
                        } else {
                            if (stockRealTimePriceByCode.isPlank()) {
                                Thread.sleep(600);
                                // 二次确认,有些票只是瞬间摸一下涨停价就回落
                                stockRealTimePriceByCode = stockProcessor.getStockRealTimePriceByCode(stock.getCode());
                                if (stockRealTimePriceByCode.isPlank()) {
                                    // 上板,下单排队
                                    int sum = 0, amount = 1;
                                    while (sum <= plankConfig.getSingleTransactionLimitAmount()) {
                                        sum = (int) (amount++ * 100 * stockRealTimePriceByCode.getCurrentPrice());
                                    }
                                    amount -= 2;
                                    if (amount >= 1) {
                                        double cost = amount * 100 * stockRealTimePriceByCode.getLimitUp();
                                        if (AutomaticTrading.TODAY_COST_MONEY.intValue() + cost < plankConfig.getAutomaticTradingMoneyLimitUp()) {
                                            automaticTrading.buy(stock, amount * 100, stockRealTimePriceByCode.getLimitUp(),
                                                    AutomaticTradingEnum.AUTO_PLANK.name());
                                        }
                                    }
                                    PLANKING_CACHE.remove(stock.getCode());
                                }
                            } else if ((stock.getName().startsWith("SZ30") && stockRealTimePriceByCode.getIncreaseRate() < 17) ||
                                    (!stock.getName().startsWith("SZ30") && stockRealTimePriceByCode.getIncreaseRate() < 7.5)) {
                                log.warn("{} 取消打板监控,当前涨幅 {}%", stock.getName(), stockRealTimePriceByCode.getIncreaseRate());
                                PLANKING_CACHE.remove(stock.getCode());
                                watch = false;
                            }
                        }
                        if (PLANKING_CACHE.size() > availableProcessors) {
                            Thread.yield();
                        }
                    } else {
                        Thread.sleep(3000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 是否开启自动打板 只打plankConfig.getAutomaticPlankTradingTimeLimit()点前封板的票
     *
     * @return boolean
     */
    public boolean openAutoPlank() {
        return AutomaticTrading.isTradeTime() &&
                DateUtil.hour(new Date(), true) < plankConfig.getAutomaticPlankTradingTimeLimit() &&
                AutomaticTrading.TODAY_COST_MONEY.intValue() < plankConfig.getAutomaticTradingMoneyLimitUp();
    }
}
