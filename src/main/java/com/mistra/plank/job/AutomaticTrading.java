package com.mistra.plank.job;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.common.util.StockUtil;
import com.mistra.plank.dao.HoldSharesMapper;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.model.dto.StockRealTimePrice;
import com.mistra.plank.model.entity.HoldShares;
import com.mistra.plank.model.entity.Stock;
import com.mistra.plank.model.enums.AutomaticTradingEnum;
import com.mistra.plank.model.enums.ClearanceReasonEnum;
import com.mistra.plank.service.TradeApiService;
import com.mistra.plank.tradeapi.TradeResultVo;
import com.mistra.plank.tradeapi.request.SubmitRequest;
import com.mistra.plank.tradeapi.response.SubmitResponse;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Mistra @ Version: 1.0
 * @ Time: 2021/11/18 22:09
 * @ Copyright (c) Mistra,All Rights Reserved
 * @ Github: https://github.com/MistraR
 * @ CSDN: https://blog.csdn.net/axela30w
 * @ Description: 自动交易任务，需要自己在数据库 stock表 手动编辑自己想要交易的股票，买入模式(打板还是低吸)，买入数量，价格
 * 买入：1.打板，发现上板则立即挂买单排板  2.低吸，发现股票价格触发到低吸价格，挂涨停价买入
 * 卖出：1.止损，跌破止损价，挂跌停价割肉  2.止盈，触发止盈标准挂跌停价卖出
 * 编辑自动交易接口地址:/tomorrow-auto-trade-pool
 * 参数：[
 *          {
 *              "name": "亿纬锂能",
 *              "automaticTradingType": "SUCK",
 *              "buyAmount": 100,
 *              "triggerPrice": 100.93
 *          }
 *      ]
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
     * 监控中的股票
     */
    public static final ConcurrentHashMap<String, Stock> UNDER_MONITORING = new ConcurrentHashMap<>();

    /**
     * 当日已经成功挂单的股票
     */
    public static final HashSet<String> TODAY_BOUGHT_SUCCESS = new HashSet<>();

    /**
     * 今日自动交易花费金额
     */
    public static AtomicInteger TODAY_COST_MONEY = new AtomicInteger(0);

    private final AtomicBoolean SELLING = new AtomicBoolean(false);

    private final ThreadPoolExecutor SALE_POOL = new ThreadPoolExecutor(10, 20, 100L, TimeUnit.SECONDS,
            new SynchronousQueue<>(), new NamedThreadFactory("止盈止损-", false));

    /**
     * 正在监控中的持仓,自动止盈止损
     */
    public static final ConcurrentHashSet<String> SALE_STOCK_CACHE = new ConcurrentHashSet<>();

    public AutomaticTrading(StockMapper stockMapper, TradeApiService tradeApiService, PlankConfig plankConfig,
                            StockProcessor stockProcessor, HoldSharesMapper holdSharesMapper) {
        this.stockMapper = stockMapper;
        this.tradeApiService = tradeApiService;
        this.plankConfig = plankConfig;
        this.stockProcessor = stockProcessor;
        this.holdSharesMapper = holdSharesMapper;
    }

    /**
     * 每1秒更新一次需要低吸的股票,需要卖出的股票线程监控
     */
    @Scheduled(cron = "*/1 * * * * ?")
    private void autoBuy() {
        if (plankConfig.getAutomaticTrading() && isTradeTime()) {
            List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>().eq(Stock::getAutomaticTradingType,
                    AutomaticTradingEnum.SUCK.name()));
            if (CollectionUtils.isNotEmpty(stocks)) {
                List<String> codes = stocks.stream().map(Stock::getCode).collect(Collectors.toList());
                for (Map.Entry<String, Stock> entry : UNDER_MONITORING.entrySet()) {
                    if (!codes.contains(entry.getKey())) {
                        UNDER_MONITORING.remove(entry.getKey());
                    }
                }
                for (Stock stock : stocks) {
                    if (!TODAY_BOUGHT_SUCCESS.contains(stock.getCode()) && !UNDER_MONITORING.containsKey(stock.getCode())) {
                        Barbarossa.executorService.submit(new BuyTask(stock));
                        UNDER_MONITORING.put(stock.getCode(), stock);
                    }
                }
            } else {
                UNDER_MONITORING.clear();
            }
            // 监控持仓,止盈止损
            List<HoldShares> holdShares = holdSharesMapper.selectList(new LambdaQueryWrapper<HoldShares>()
                    .gt(HoldShares::getAvailableVolume, 0).eq(HoldShares::getClearance, false));
            if (CollectionUtils.isNotEmpty(holdShares)) {
                if (!SELLING.get()) {
                    for (HoldShares holdShare : holdShares) {
                        SALE_POOL.submit(new SaleTask(holdShare));
                    }
                }
                SELLING.set(true);
            }
        } else {
            SELLING.set(false);
        }
    }

    /**
     * 当前时间是否是交易时间 只判定了时分秒，没有判定非交易日（周末及法定节假日），因为我一般只交易日才会启动项目
     *
     * @return 是否是交易时间
     */
    public static boolean isTradeTime() {
        int week = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
        if (week == 6 || week == 0) {
            //0-周日,6-周六
            return false;
        }
        int hour = DateUtil.hour(new Date(), true);
        return (hour == 9 && DateUtil.minute(new Date()) >= 30) || (hour == 11 && DateUtil.minute(new Date()) < 30)
                || hour == 10 || hour == 13 || hour == 14;
    }

    @Override
    public void run(String... args) {
        List<HoldShares> shares = holdSharesMapper.selectList(new LambdaQueryWrapper<HoldShares>().eq(HoldShares::getAutomaticTradingType,
                AutomaticTradingEnum.AUTO_PLANK.name()).ge(HoldShares::getBuyTime, DateUtil.beginOfDay(new Date())));
        for (HoldShares share : shares) {
            TODAY_COST_MONEY.set((int) (TODAY_COST_MONEY.intValue() + share.getBuyPrice().doubleValue() * share.getNumber()));
            TODAY_BOUGHT_SUCCESS.add(share.getCode());
        }
    }

    class SaleTask implements Runnable {

        private HoldShares holdShare;

        private final String name;

        public SaleTask(HoldShares holdShare) {
            this.holdShare = holdShare;
            this.name = holdShare.getName();
        }

        /**
         * 自动交易的止盈止损策略
         */
        @Override
        public void run() {
            while (Objects.nonNull(holdShare) && holdShare.getAvailableVolume() > 0) {
                SALE_STOCK_CACHE.add(name);
                try {
                    if (isTradeTime()) {
                        holdShare = holdSharesMapper.selectById(holdShare.getId());
                        if (Objects.isNull(holdShare) || holdShare.getAvailableVolume() <= 0) {
                            break;
                        }
                        StockRealTimePrice stockRealTimePrice = stockProcessor.getStockRealTimePriceByCode(holdShare.getCode());
                        if (stockRealTimePrice.getCurrentPrice() <= holdShare.getStopLossPrice().doubleValue()) {
                            log.error("{} 触发止损,自动卖出", holdShare.getName());
                            sale(holdShare, stockRealTimePrice, ClearanceReasonEnum.STOP_LOSE);
                            break;
                        }
                        if ((stockRealTimePrice.getHighestPrice().doubleValue() == stockRealTimePrice.getLimitUp().doubleValue()
                                || stockRealTimePrice.isPlank()) && !holdShare.getTodayPlank()) {
                            log.error("{} 首次封板", holdShare.getName());
                            holdShare.setTodayPlank(true);
                        }
                        if (holdShare.getTodayPlank() && !stockRealTimePrice.isPlank()) {
                            log.error("{} 炸板,自动卖出", holdShare.getName());
                            sale(holdShare, stockRealTimePrice, ClearanceReasonEnum.RATTY_PLANK);
                            break;
                        }
                        if (holdShare.getTakeProfitPrice().doubleValue() <= stockRealTimePrice.getHighestPrice()) {
                            sale(holdShare, stockRealTimePrice, ClearanceReasonEnum.TAKE_PROFIT);
                            log.error("{} 触发止盈,自动卖出", holdShare.getName());
                            break;
                        }
                        // 当前盈利
                        holdShare.setProfit(BigDecimal.valueOf((stockRealTimePrice.getCurrentPrice() - holdShare.getBuyPrice().doubleValue())
                                * holdShare.getAvailableVolume()));
                        BigDecimal rate = divide(stockRealTimePrice.getCurrentPrice() - holdShare.getBuyPrice().doubleValue(),
                                holdShare.getBuyPrice().doubleValue());
                        if (holdShare.getHighestProfitRatio().doubleValue() < rate.doubleValue()) {
                            holdShare.setHighestProfitRatio(rate);
                        }
                        if (holdShare.getAutomaticTradingType().equals(AutomaticTradingEnum.AUTO_PLANK.name())) {
                            // 自动打板买入的股票,11点前还未涨停,自动卖出
                            if (DateUtil.hour(new Date(), true) > 14 && !stockRealTimePrice.isPlank()) {
                                log.error("{} 14点前还未涨停,自动卖出", holdShare.getName());
                                sale(holdShare, stockRealTimePrice, ClearanceReasonEnum.UN_PLANK);
                                break;
                            }
                            if (holdShare.getHighestProfitRatio().doubleValue() > 0.04 && stockRealTimePrice.getCurrentPrice() < holdShare.getBuyPrice().doubleValue() + 0.01) {
                                // 动态调整止损位,由盈利4%以上到回撤到触及成本,自动卖出
                                sale(holdShare, stockRealTimePrice, ClearanceReasonEnum.ROLLER_COASTER);
                            }
                            if (rate.doubleValue() > 0.02 && holdShare.getHighestProfitRatio().doubleValue() - rate.doubleValue() > 0.03) {
                                // 从今日最高点回落3个点,清仓
                                sale(holdShare, stockRealTimePrice, ClearanceReasonEnum.RETRACEMENT);
                            }
                        }
                        holdSharesMapper.updateById(holdShare);
                        Thread.sleep(200);
                    } else {
                        Thread.sleep(3000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            SALE_STOCK_CACHE.remove(name);
        }
    }

    private BigDecimal divide(double x, double y) {
        return y <= 0 ? new BigDecimal(0) : new BigDecimal(x).divide(new BigDecimal(y), 2, RoundingMode.HALF_UP);
    }

    private void sale(HoldShares holdShare, StockRealTimePrice stockRealTimePrice, ClearanceReasonEnum reason) {
        SubmitRequest request = new SubmitRequest(1);
        request.setAmount(holdShare.getAvailableVolume());
//        request.setPrice(stockRealTimePrice.getLimitDown());
        // 全面注册制后只能最多挂-2%价格卖单
        request.setPrice(BigDecimal.valueOf(stockRealTimePrice.getCurrentPrice() * 0.985).setScale(2, RoundingMode.HALF_UP).doubleValue());
        request.setStockCode(holdShare.getCode().substring(2, 8));
        request.setZqmc(holdShare.getName());
        request.setTradeType(SubmitRequest.S);
        request.setMarket(StockUtil.getStockMarket(request.getStockCode()));
        TradeResultVo<SubmitResponse> response = tradeApiService.submit(request);
        holdShare.setAvailableVolume(0);
        holdShare.setClearance(true);
        holdShare.setClearanceReason(reason.name());
        holdShare.setSaleTime(new Date());
        holdShare.setProfit(BigDecimal.valueOf((stockRealTimePrice.getCurrentPrice() - holdShare.getBuyPrice().doubleValue())
                * holdShare.getNumber()));
        holdSharesMapper.updateById(holdShare);
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getName, holdShare.getName()));
        stock.setShareholding(false);
        stock.setAutomaticTradingType(AutomaticTradingEnum.CANCEL.name());
        stockMapper.updateById(stock);
        if (response.success()) {
            log.error("触发{}止盈、止损，交易成功!", holdShare.getName());
            holdShare = null;
        } else {
            log.error("触发{}止盈、止损，交易失败!", holdShare.getName());
        }
    }

    class BuyTask implements Runnable {

        private final Stock stock;

        public BuyTask(Stock stock) {
            this.stock = stock;
        }

        @Override
        public void run() {
            while (!TODAY_BOUGHT_SUCCESS.contains(stock.getCode()) && isTradeTime() &&
                    UNDER_MONITORING.containsKey(stock.getCode()) &&
                    TODAY_COST_MONEY.intValue() < plankConfig.getAutomaticTradingMoneyLimitUp()) {
                try {
                    automaticTrading(stock);
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void automaticTrading(Stock stock) {
        StockRealTimePrice stockRealTimePrice = stockProcessor.getStockRealTimePriceByCode(stock.getCode());
        if (Objects.nonNull(stockRealTimePrice)) {
            if (stock.getAutomaticTradingType().equals(AutomaticTradingEnum.SUCK.name()) &&
                    stockRealTimePrice.getCurrentPrice() <= stock.getSuckTriggerPrice().doubleValue()) {
                // 触发低吸下单条件，挂单
                buy(stock, stock.getBuyAmount(), stockRealTimePrice.getLimitUp(), stock.getAutomaticTradingType());
            }
        }
    }

    /**
     * 挂单
     *
     * @param stock                Stock
     * @param amount               买入数量
     * @param price                买入价格
     * @param automaticTradingType 自动交易类型
     */
    public boolean buy(Stock stock, int amount, double price, String automaticTradingType) {
        if (!TODAY_BOUGHT_SUCCESS.contains(stock.getCode()) && this.buy(stock, amount, price)) {
            TODAY_BOUGHT_SUCCESS.add(stock.getCode());
            UNDER_MONITORING.remove(stock.getCode());
            stock.setAutomaticTradingType(AutomaticTradingEnum.CANCEL.name());
            stock.setBuyTime(new Date());
            stock.setShareholding(true);
            stockMapper.updateById(stock);
            // 新增持仓数据,打板单有可能截止收盘都未成交,该持仓数据需要手动删除作废
            HoldShares holdShare = HoldShares.builder().buyTime(new Date()).clearance(false).code(stock.getCode())
                    .name(stock.getName()).availableVolume(0).number(amount).profit(new BigDecimal(0))
                    // 设置触发止损价
                    .stopLossPrice(BigDecimal.valueOf(price * plankConfig.getStopLossRate()).setScale(2, RoundingMode.HALF_UP))
                    // 设置触发止盈价
                    .takeProfitPrice(BigDecimal.valueOf(price * plankConfig.getTakeProfitRate()).setScale(2, RoundingMode.HALF_UP))
                    .automaticTradingType(automaticTradingType).buyPrice(BigDecimal.valueOf(price)).highestProfitRatio(new BigDecimal(0)).build();
            holdSharesMapper.insert(holdShare);
            return true;
        }
        return false;
    }

    /**
     * 东财挂单接口
     *
     * @param stock  Stock
     * @param amount amount
     * @param price  price
     * @return 挂单是否成功
     */
    private boolean buy(Stock stock, int amount, double price) {
        SubmitRequest request = new SubmitRequest(1);
        request.setAmount(amount);
        request.setPrice(price);
        request.setStockCode(stock.getCode().substring(2, 8));
        request.setZqmc(stock.getName());
        request.setTradeType(SubmitRequest.B);
        request.setMarket(StockUtil.getStockMarket(request.getStockCode()));
        TradeResultVo<SubmitResponse> response = tradeApiService.submit(request);
        if (response.success()) {
            // 打板排队有可能只是排单，并没有成交
            log.error("成功下单[{}],数量:{},价格:{}", stock.getName(), amount, price);
            TODAY_COST_MONEY.set((int) (TODAY_COST_MONEY.intValue() + price * amount));
        } else {
            log.error("下单[{}]失败,message:{}", stock.getName(), response.getMessage());
        }
        return response.success();
    }
}
