package com.mistra.plank.job;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.model.dto.StockRealTimePrice;
import com.mistra.plank.model.entity.Stock;
import com.mistra.plank.model.enums.AutomaticTradingEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author rui.wang
 * @ Version: 1.0
 * @ Time: 2023/2/15 13:17
 * @ Description: 自动打板交易，开启的话会监控成交额大于3亿(PlankConfig.stockTurnoverFilter)的全市场股票，发现上板则会自动下单排队
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
    private final StockMapper stockMapper;

    /**
     * 自动打板股票,一级过滤map
     */
    public static final HashMap<String, Stock> STOCK_AUTO_PLANK_FILTER_MAP = new HashMap<>(512);

    /**
     * 自动打板股票,二级过滤map
     * 主板涨幅大于7个点股票，创业板涨幅大于18个点的股票，上板则下单排队
     * 涨幅小于6个点或16个点会被移除
     * 想打板哪个股票，放入这个map就会自动监控，上板就会下单
     */
    public static final ConcurrentHashMap<String, Stock> PLANK_MONITOR = new ConcurrentHashMap<>();

    public AutomaticPlankTrading(PlankConfig plankConfig, StockProcessor stockProcessor,
                                 AutomaticTrading automaticTrading, StockMapper stockMapper) {
        this.plankConfig = plankConfig;
        this.stockProcessor = stockProcessor;
        this.automaticTrading = automaticTrading;
        this.stockMapper = stockMapper;
    }

    /**
     * 每5秒过滤主板涨幅大于7个点股票，创业板涨幅大于18个点的股票，放入PLANK_MONITOR
     */
    @Scheduled(cron = "*/4 * * * * ?")
    private void filterStock() {
        if (openAutoPlank() && Barbarossa.executorService.getQueue().size() < 32) {
            List<List<String>> lists = Lists.partition(Lists.newArrayList(STOCK_AUTO_PLANK_FILTER_MAP.keySet()),
                    Barbarossa.executorService.getMaximumPoolSize() / 2);
            for (List<String> list : lists) {
                Barbarossa.executorService.submit(() -> filterStock(list));
            }
        }
    }

    /**
     * 过滤主板涨幅大于7个点的股票，创业板大于17个点的股票
     *
     * @param codes codes
     */
    private void filterStock(List<String> codes) {
        codes.forEach(e -> {
            StockRealTimePrice stockRealTimePriceByCode = stockProcessor.getStockRealTimePriceByCode(e);
            if ((stockRealTimePriceByCode.getCode().contains("SZ30") && stockRealTimePriceByCode.getIncreaseRate() > 17) ||
                    (!stockRealTimePriceByCode.getCode().contains("SZ30") && stockRealTimePriceByCode.getIncreaseRate() > 7)) {
                double v = stockRealTimePriceByCode.getCurrentPrice() * 100;
                if (v <= plankConfig.getSingleTransactionLimitAmount() &&
                        AutomaticTrading.TODAY_COST_MONEY.intValue() + v < plankConfig.getAutomaticTradingMoneyLimitUp() &&
                        !PLANK_MONITOR.containsKey(e)) {
                    PLANK_MONITOR.put(e, STOCK_AUTO_PLANK_FILTER_MAP.get(e));
                    log.warn("{} 新加入打板监测", e);
                }
                if (AutomaticTrading.TODAY_BOUGHT_SUCCESS.contains(e)) {
                    PLANK_MONITOR.remove(e);
                }
            }
        });
    }

    /**
     * 一直监控主板涨幅大于7个点股票，创业板涨幅大于18个点的股票,即PLANK_MONITOR中的股票
     */
    @Override
    public void run(String... args) {
        filterStock();
        yesterdayPlank();
        Barbarossa.executorService.submit(this::autoPlank);
    }

    /**
     * 昨日2板3板股票加入监测池,上板则排单
     */
    private void yesterdayPlank() {
        List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>().in(Stock::getPlankNumber, 2, 3));
        for (Stock stock : stocks) {
            PLANK_MONITOR.put(stock.getCode(), stock);
        }
    }

    /**
     * 只打当日涨幅Top5的版块的成分股，并且10点(plankConfig.getAutomaticPlankTradingTimeLimit())以前涨停的股票
     */
    private void autoPlank() {
        while (openAutoPlank()) {
            try {
                if (!PLANK_MONITOR.isEmpty()) {
                    for (Stock stock : PLANK_MONITOR.values()) {
                        StockRealTimePrice stockRealTimePriceByCode = stockProcessor.getStockRealTimePriceByCode(stock.getCode());
                        if (stockRealTimePriceByCode.isPlank()) {
                            // 上板,下单排队
                            STOCK_AUTO_PLANK_FILTER_MAP.remove(stock.getCode());
                            PLANK_MONITOR.remove(stock.getCode());
                            int sum = 0, amount = 1;
                            while (sum <= plankConfig.getSingleTransactionLimitAmount()) {
                                sum = (int) (amount++ * 100 * stockRealTimePriceByCode.getCurrentPrice());
                            }
                            amount -= 2;
                            if (amount >= 1) {
                                automaticTrading.buy(stock, amount * 100, stockRealTimePriceByCode.getLimitUp(),
                                        AutomaticTradingEnum.AUTO_PLANK.name());
                            }
                        } else if ((stockRealTimePriceByCode.getCode().contains("SZ30") && stockRealTimePriceByCode.getIncreaseRate() < 16) ||
                                (!stockRealTimePriceByCode.getCode().contains("SZ30") && stockRealTimePriceByCode.getIncreaseRate() < 6)) {
                            PLANK_MONITOR.remove(stock.getCode());
                        }
                    }
                    Thread.sleep(200);
                } else {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.warn("------------------------ 终止打板监测 ------------------------");
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
