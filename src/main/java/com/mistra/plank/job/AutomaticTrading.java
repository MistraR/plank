package com.mistra.plank.job;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.common.util.StockUtil;
import com.mistra.plank.dao.HoldSharesMapper;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.model.entity.HoldShares;
import com.mistra.plank.model.entity.Stock;
import com.mistra.plank.model.enums.AutomaticTradingEnum;
import com.mistra.plank.model.enums.HoldSharesEnum;
import com.mistra.plank.service.TradeApiService;
import com.mistra.plank.tradeapi.TradeResultVo;
import com.mistra.plank.tradeapi.request.SubmitRequest;
import com.mistra.plank.tradeapi.response.SubmitResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author rui.wang
 * @ Version: 1.0
 * @ Time: 2022/11/1 14:18
 * @ Description: 自动交易任务
 * 买入：1.打板，发现上板则立即挂买单排板  2.低吸，发现股票价格触发到低吸价格，挂涨停价买入
 * 卖出：1.止损，跌破止损价，挂跌停价割肉  2.止盈，触发止盈标准挂跌停价卖出
 * 需要自己在数据库 stock表 手动编辑自己想要监控打板的股票，买入数量，价格
 */
@Slf4j
@Component
public class AutomaticTrading implements CommandLineRunner {

    private final StockMapper stockMapper;
    private final TradeApiService tradeApiService;

    private final PlankConfig plankConfig;
    private final StockProcessor stockProcessor;
    private final HoldSharesMapper holdSharesMapper;

    /**
     * 需要监控的股票
     */
    private static final ConcurrentHashMap<String, Stock> map = new ConcurrentHashMap<>();

    /**
     * 监控中的股票
     */
    public static final ConcurrentHashMap<String, Stock> runningMap = new ConcurrentHashMap<>();

    /**
     * 已经成功挂单的股票
     */
    private static final HashSet<String> pendingOrderSet = new HashSet<>();

    private final ReentrantLock lock = new ReentrantLock();

    public AutomaticTrading(StockMapper stockMapper, TradeApiService tradeApiService, PlankConfig plankConfig,
                            StockProcessor stockProcessor, HoldSharesMapper holdSharesMapper) {
        this.stockMapper = stockMapper;
        this.tradeApiService = tradeApiService;
        this.plankConfig = plankConfig;
        this.stockProcessor = stockProcessor;
        this.holdSharesMapper = holdSharesMapper;
    }

    /**
     * 每3秒更新一次需要打板的股票
     */
    @Scheduled(cron = "*/30 * * * * ?")
    private void plank() {
        if (plankConfig.getAutomaticTrading() && isTradeTime()) {
            List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>().in(Stock::getAutomaticTradingType,
                    AutomaticTradingEnum.PLANK.name(), AutomaticTradingEnum.SUCK.name()));
            lock.lock();
            try {
                map.clear();
                for (Stock stock : stocks) {
                    if (!pendingOrderSet.contains(stock.getCode())) {
                        map.put(stock.getCode(), stock);
                        if (!runningMap.containsKey(stock.getCode())) {
                            Barbarossa.executorService.submit(new BuyTask(stock));
                            runningMap.put(stock.getCode(), stock);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

    @Scheduled(cron = "0 2 15 * * ?")
    private void updateHoldShares() {
        List<HoldShares> holdShares = holdSharesMapper.selectList(new LambdaQueryWrapper<HoldShares>()
                .ge(HoldShares::getBuyTime, DateUtil.beginOfDay(new Date()))
                .le(HoldShares::getBuyTime, DateUtil.endOfDay(new Date())));
        if (CollectionUtils.isNotEmpty(holdShares)) {
            for (HoldShares holdShare : holdShares) {
                holdShare.setAvailableVolume(holdShare.getBuyNumber());
                holdSharesMapper.updateById(holdShare);
            }
        }
    }

    /**
     * 当前时间是否是交易时间
     * <p>
     * 只判定了时分秒，没有判定非交易日（周末及法定节假日），因为我一般只交易日才会启动项目
     *
     * @return boolean
     */
    public static boolean isTradeTime() {
        int week = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
        if (week == 6 || week == 0) {
            //0代表周日，6代表周六
            return false;
        }
        int hour = DateUtil.hour(new Date(), true);
        return (hour == 9 && DateUtil.minute(new Date()) > 30) || (hour == 11 && DateUtil.minute(new Date()) <= 29)
                || hour == 10 || hour == 13 || hour == 14;
    }

    @Override
    public void run(String... args) throws Exception {
        // 自动买入
        plank();
        // 监控持仓，止盈止损
        List<HoldShares> holdShares = holdSharesMapper.selectList(new LambdaQueryWrapper<HoldShares>()
                .eq(HoldShares::getType, HoldSharesEnum.REALITY.name())
                .ge(HoldShares::getAvailableVolume, 0));
        if (CollectionUtils.isNotEmpty(holdShares)) {
            for (HoldShares holdShare : holdShares) {
                Barbarossa.executorService.submit(new SaleTask(holdShare));
            }
        }
    }

    class SaleTask implements Runnable {

        private final HoldShares holdShare;

        public SaleTask(HoldShares holdShare) {
            this.holdShare = holdShare;
        }

        @Override
        public void run() {
            while (isTradeTime()) {
                try {
                    HoldShares data = holdSharesMapper.selectById(holdShare.getId());
                    if (Objects.nonNull(data)) {
                        double price = stockProcessor.getStockRealTimePriceByCode(data.getCode()).getTodayRealTimePrice();
                        if (price <= data.getStopLossPrice().doubleValue() || data.getTakeProfitPrice().doubleValue() <= price) {
                            // 触发止盈、止损，挂跌停价卖出
                            SubmitRequest request = new SubmitRequest(1);
                            request.setAmount(data.getAvailableVolume());
                            request.setPrice(data.getSalePrice().doubleValue());
                            request.setStockCode(data.getCode().substring(2, 8));
                            request.setZqmc(data.getName());
                            request.setTradeType(SubmitRequest.S);
                            request.setMarket(StockUtil.getStockMarket(request.getStockCode()));
                            TradeResultVo<SubmitResponse> response = tradeApiService.submit(request);
                            data.setAvailableVolume(0);
                            holdSharesMapper.updateById(data);
                            if (response.success()) {
                                log.warn("触发{}止盈、止损，交易成功!", data.getName());
                            } else {
                                log.error("触发{}止盈、止损，交易失败!", data.getName());
                                Thread.sleep(1000 * 60);
                            }
                            break;
                        }
                        Thread.sleep(200);
                    } else {
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class BuyTask implements Runnable {

        private final Stock stock;

        private final AtomicBoolean buy = new AtomicBoolean(false);

        public BuyTask(Stock stock) {
            this.stock = stock;
        }

        @Override
        public void run() {
            while (!buy.get() && isTradeTime()) {
                try {
                    automaticTrading(stock, buy);
                    Thread.sleep(200);
                    if (!lock.isLocked()) {
                        if (!map.containsKey(stock.getCode())) {
                            runningMap.remove(stock.getCode());
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void automaticTrading(Stock stock, AtomicBoolean buy) {
        double price = stockProcessor.getStockRealTimePriceByCode(stock.getCode()).getTodayRealTimePrice();
        if (price != 0d) {
            if (stock.getAutomaticTradingType().equals(AutomaticTradingEnum.PLANK.name()) && price >= stock.getTriggerPrice().doubleValue()) {
                // 触发打板下单条件，挂单
                buy(stock, buy, price);
            } else if (stock.getAutomaticTradingType().equals(AutomaticTradingEnum.SUCK.name()) && price <= stock.getTriggerPrice().doubleValue()) {
                // 触发低吸下单条件，挂单
                buy(stock, buy, price);
            }
        }
    }

    /**
     * 下单
     *
     * @param stock Stock
     * @param buy   AtomicBoolean
     */
    private void buy(Stock stock, AtomicBoolean buy, double currentPrice) {
        SubmitRequest request = new SubmitRequest(1);
        request.setAmount(stock.getBuyAmount());
        request.setPrice(stock.getBuyPrice().doubleValue());
        request.setStockCode(stock.getCode().substring(2, 8));
        request.setZqmc(stock.getName());
        request.setTradeType(SubmitRequest.B);
        request.setMarket(StockUtil.getStockMarket(request.getStockCode()));
        TradeResultVo<SubmitResponse> response = tradeApiService.submit(request);
        if (response.success()) {
            runningMap.remove(stock.getCode());
            map.remove(stock.getCode());
            pendingOrderSet.add(stock.getCode());
            buy.set(true);
            // 已经挂单，就修改为不监控该股票了
            stock.setAutomaticTradingType(AutomaticTradingEnum.CANCEL.name());
            stock.setBuyTime(new Date());
            stockMapper.updateById(stock);
            HoldShares holdShare = HoldShares.builder().buyTime(new Date())
                    .code(stock.getCode()).name(stock.getName()).cost(BigDecimal.valueOf(currentPrice)).availableVolume(0)
                    .fifteenProfit(false).number(stock.getBuyAmount()).profit(new BigDecimal(0)).buyTime(new Date())
                    .stopLossPrice(BigDecimal.valueOf(currentPrice * 0.95).setScale(2, RoundingMode.HALF_UP))
                    .takeProfitPrice(BigDecimal.valueOf(currentPrice * 1.07).setScale(2, RoundingMode.HALF_UP))
                    .salePrice(BigDecimal.valueOf(currentPrice * 0.91).setScale(2, RoundingMode.HALF_UP))
                    .currentPrice(BigDecimal.valueOf(currentPrice)).rate(new BigDecimal(0)).type(HoldSharesEnum.REALITY.name())
                    .buyPrice(BigDecimal.valueOf(currentPrice)).buyNumber(stock.getBuyAmount()).build();
            holdSharesMapper.insert(holdShare);
            log.info("成功下单[{}],数量:{},价格:{}", stock.getName(), stock.getBuyAmount(), stock.getBuyPrice().doubleValue());
        } else {
            log.error("下单[{}]失败,message:{}", stock.getName(), response.getMessage());
        }
    }
}
