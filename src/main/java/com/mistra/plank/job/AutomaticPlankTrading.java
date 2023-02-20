package com.mistra.plank.job;

import cn.hutool.core.collection.ConcurrentHashSet;
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
import java.util.List;
import java.util.Objects;

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
     * 自动打板股票，二级过滤map
     * 主板涨幅大于7个点股票，创业板涨幅大于18个点的股票，上板则下单排队
     */
    private static final ConcurrentHashSet<String> PLANK_MONITOR = new ConcurrentHashSet<>();

    public AutomaticPlankTrading(PlankConfig plankConfig, StockProcessor stockProcessor,
                                 AutomaticTrading automaticTrading, StockMapper stockMapper) {
        this.plankConfig = plankConfig;
        this.stockProcessor = stockProcessor;
        this.automaticTrading = automaticTrading;
        this.stockMapper = stockMapper;
    }

    /**
     * 每2秒过滤主板涨幅大于7个点股票，创业板涨幅大于18个点的股票，放入PLANK_MONITOR
     */
    @Scheduled(cron = "*/2 * * * * ?")
    private void filterStock() {
        if (openAutoPlank()) {
            List<List<String>> lists = Lists.partition(Lists.newArrayList(Barbarossa.STOCK_FILTER_MAP.keySet()),
                    Barbarossa.executorService.getMaximumPoolSize() / 2);
            for (List<String> list : lists) {
                Barbarossa.executorService.submit(() -> filterStock(list));
            }
            log.warn("当前打板监测股票:{}", PLANK_MONITOR);
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
                        AutomaticTrading.TODAY_COST_MONEY.intValue() + v < plankConfig.getAutomaticTradingMoneyLimitUp()) {
                    PLANK_MONITOR.add(e);
                    if (AutomaticTrading.TODAY_BOUGHT_SUCCESS.contains(e)) {
                        PLANK_MONITOR.remove(e);
                    }
                    log.warn("{} 新加入打板监测,当前共监测:{}支股票", e, PLANK_MONITOR.size());
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
        Barbarossa.executorService.submit(this::autoPlank);
    }

    /**
     * 只打当日涨幅Top5的版块的成分股，并且10点(plankConfig.getAutomaticPlankTradingTimeLimit())以前涨停的股票
     */
    private void autoPlank() {
        while (openAutoPlank()) {
            try {
                if (!PLANK_MONITOR.isEmpty()) {
                    for (String code : PLANK_MONITOR) {
                        StockRealTimePrice stockRealTimePriceByCode = stockProcessor.getStockRealTimePriceByCode(code);
                        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                                .eq(Stock::getCode, stockRealTimePriceByCode.getCode())
                                .ne(Stock::getClassification, "")
                                .and(wrapper -> wrapper.isNull(Stock::getBuyTime).or().le(Stock::getBuyTime, DateUtil.beginOfDay(new Date()))));
                        if (stockRealTimePriceByCode.isPlank() && Objects.nonNull(stock)) {
                            for (String bk : StockProcessor.TOP5_BK.keySet()) {
                                if (stock.getClassification().contains(bk)) {
                                    Barbarossa.STOCK_FILTER_MAP.remove(code);
                                    PLANK_MONITOR.remove(code);
                                    log.warn("准备挂单[{}],所属版块:{} {}", stock.getName(), StockProcessor.TOP5_BK.get(bk).getName(),
                                            StockProcessor.TOP5_BK.get(bk).getIncreaseRate());
                                    // 上板，下单排队
                                    int sum = 0, amount = 1;
                                    while (sum <= plankConfig.getSingleTransactionLimitAmount()) {
                                        sum = (int) (amount++ * 100 * stockRealTimePriceByCode.getCurrentPrice());
                                    }
                                    amount -= 2;
                                    if (amount >= 1) {
                                        automaticTrading.buy(stock, amount * 100, stockRealTimePriceByCode.getLimitUp(),
                                                AutomaticTradingEnum.AUTO_PLANK.name());
                                    }
                                    break;
                                }
                            }
                        } else if (stockRealTimePriceByCode.getIncreaseRate() < 5) {
                            PLANK_MONITOR.remove(code);
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
    private boolean openAutoPlank() {
        return AutomaticTrading.isTradeTime() &&
                DateUtil.hour(new Date(), true) < plankConfig.getAutomaticPlankTradingTimeLimit() &&
                AutomaticTrading.TODAY_COST_MONEY.intValue() < plankConfig.getAutomaticTradingMoneyLimitUp();
    }
}
