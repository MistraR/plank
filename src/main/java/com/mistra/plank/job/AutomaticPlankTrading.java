package com.mistra.plank.job;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
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
 * @author rui.wang
 * @ Version: 1.0
 * @ Time: 2023/2/15 13:17
 * @ Description:
 * 根据不同策略筛选出来的股票 新开一个线程 Barbarossa.executorService.submit(new AutoPlankTask(stock)); 发现上板则会自动下单排队
 * 目前有：
 * 1.创业板首板 - 隔日溢价率挺高的 filterStock()
 * 2.自己复盘筛选的打板标的 - 筛选出人气龙头 selectAutoPlankStock()
 * 3.隔日一进二，二进三
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
     * 打板监控缓存
     */
    public static final ConcurrentHashMap<String, Stock> PLANK_MONITOR = new ConcurrentHashMap<>();

    public static final ThreadPoolExecutor PLANK_POOL = new ThreadPoolExecutor(availableProcessors, availableProcessors, 100L,
            TimeUnit.SECONDS, new LinkedBlockingQueue<>(5000), new NamedThreadFactory("Plank-", false));

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
        if (openAutoPlank() && Barbarossa.executorService.getQueue().size() < 32) {
            Set<String> todayBufSaleSet = selectTodayTradedStock();
            List<List<String>> lists =
                    Lists.partition(Lists.newArrayList(Barbarossa.SZ30_STOCKS.keySet().stream().filter(e -> !todayBufSaleSet.contains(e)).collect(Collectors.toSet())), Barbarossa.executorService.getMaximumPoolSize() / 2);
            for (List<String> list : lists) {
                Barbarossa.executorService.submit(() -> filterStock(list));
            }
        }
    }

    /**
     * 过滤创业板涨幅大于17个点的股票，放入打板监控缓存
     *
     * @param codes codes
     */
    private void filterStock(List<String> codes) {
        codes.forEach(e -> {
            StockRealTimePrice stockRealTimePriceByCode = stockProcessor.getStockRealTimePriceByCode(e);
            if (stockRealTimePriceByCode.getIncreaseRate() > 17 && !PLANK_MONITOR.containsKey(e)) {
                PLANK_POOL.submit(new AutoPlankTask(Barbarossa.SZ30_STOCKS.get(e)));
            }
        });
    }

    /**
     * 获取今日交易过(买入,卖出)的股票code
     *
     * @return Set<String> todayBufSaleSet = selectTodayBufSaleSet();
     */
    private Set<String> selectTodayTradedStock() {
        List<HoldShares> holdShares = holdSharesMapper.selectList(new LambdaQueryWrapper<HoldShares>().ge(HoldShares::getSaleTime,
                DateUtil.beginOfDay(new Date())).or().ge(HoldShares::getBuyTime, DateUtil.beginOfDay(new Date())));
        return holdShares.stream().map(HoldShares::getCode).collect(Collectors.toSet());
    }

    /**
     * 查询需要自动打板的股票
     */
    private void selectAutoPlankStock() {
        List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>().eq(Stock::getAutoPlank, true));
        Set<String> todayBufSaleSet = selectTodayTradedStock();
        for (Stock stock : stocks) {
            if (!todayBufSaleSet.contains(stock.getCode())) {
                PLANK_POOL.submit(new AutoPlankTask(stock));
            }
        }
    }

    /**
     * 每个打板标的一个线程，尽最快速度获取实时价格与挂单
     *
     * 打板标的数量不宜超过当前可用CPU核心数，系统其他获取数据的线程也会消耗CPU资源
     */
    class AutoPlankTask implements Runnable {

        private final Stock stock;

        public AutoPlankTask(Stock stock) {
            this.stock = stock;
        }

        @Override
        public void run() {
            if (PLANK_MONITOR.containsKey(stock.getCode())) {
                return;
            }
            log.warn("{} 新加入打板监测", stock.getName());
            boolean watch = true;
            while (watch) {
                PLANK_MONITOR.put(stock.getCode(), stock);
                try {
                    if (openAutoPlank()) {
                        StockRealTimePrice stockRealTimePriceByCode = stockProcessor.getStockRealTimePriceByCode(stock.getCode());
                        double v = stockRealTimePriceByCode.getCurrentPrice() * 100;
                        if (AutomaticTrading.TODAY_COST_MONEY.intValue() + v > plankConfig.getAutomaticTradingMoneyLimitUp()) {
                            log.warn("今日自动买入金额已接近上限:{},取消打板监控:{}", AutomaticTrading.TODAY_COST_MONEY.intValue(), stock.getName());
                            PLANK_MONITOR.remove(stock.getCode());
                            watch = false;
                        } else {
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
                                PLANK_MONITOR.remove(stock.getCode());
                            } else if (stockRealTimePriceByCode.getIncreaseRate() < 17) {
                                log.warn("{} 取消打板监控", stock.getName());
                                PLANK_MONITOR.remove(stock.getCode());
                                watch = false;
                            }
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
     * 是否开启自动打板
     * 只打plankConfig.getAutomaticPlankTradingTimeLimit()点前封板的票
     *
     * @return boolean
     */
    public boolean openAutoPlank() {
        return AutomaticTrading.isTradeTime() &&
                DateUtil.hour(new Date(), true) < plankConfig.getAutomaticPlankTradingTimeLimit() &&
                AutomaticTrading.TODAY_COST_MONEY.intValue() < plankConfig.getAutomaticTradingMoneyLimitUp();
    }
}
