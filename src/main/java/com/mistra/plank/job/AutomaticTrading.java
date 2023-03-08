package com.mistra.plank.job;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.common.util.StockUtil;
import com.mistra.plank.dao.HoldSharesMapper;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.model.dto.StockRealTimePrice;
import com.mistra.plank.model.entity.HoldShares;
import com.mistra.plank.model.entity.Stock;
import com.mistra.plank.model.enums.AutomaticTradingEnum;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author rui.wang
 * @ Version: 1.0
 * @ Time: 2022/11/1 14:18
 * @ Description: 自动交易任务，需要自己在数据库 stock表 手动编辑自己想要交易的股票，买入模式(打板还是低吸)，买入数量，价格，暂时还没有UI页面供操作
 * 买入：1.打板，发现上板则立即挂买单排板  2.低吸，发现股票价格触发到低吸价格，挂涨停价买入
 * 卖出：1.止损，跌破止损价，挂跌停价割肉  2.止盈，触发止盈标准挂跌停价卖出
 * 编辑自动交易接口地址:/tomorrow-auto-trade-pool
 * 参数：[
 * {
 * "name": "亿纬锂能",
 * "automaticTradingType": "PLANK",
 * "buyAmount": 100,
 * "triggerPrice": 100.93
 * }
 * ]
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

    public AutomaticTrading(StockMapper stockMapper, TradeApiService tradeApiService, PlankConfig plankConfig,
                            StockProcessor stockProcessor, HoldSharesMapper holdSharesMapper) {
        this.stockMapper = stockMapper;
        this.tradeApiService = tradeApiService;
        this.plankConfig = plankConfig;
        this.stockProcessor = stockProcessor;
        this.holdSharesMapper = holdSharesMapper;
    }

    /**
     * 每3秒更新一次需要打板或低吸的股票,需要卖出的股票线程监控
     */
    @Scheduled(cron = "*/3 * * * * ?")
    private void autoBuy() {
        if (plankConfig.getAutomaticTrading() && isTradeTime()) {
            List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>().in(Stock::getAutomaticTradingType,
                    AutomaticTradingEnum.PLANK.name(), AutomaticTradingEnum.SUCK.name()));
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
                        Barbarossa.executorService.submit(new SaleTask(holdShare));
                    }
                }
                SELLING.set(true);
            }
        } else {
            SELLING.set(false);
        }
    }

    /**
     * 防止忘记当日复盘，每天收盘后取消掉当天未成交的自动交易的监测
     * 已成交的更新持仓可用数量
     */
    @Scheduled(cron = "0 2 15 * * ?")
    private void updateHoldShares() {
        List<HoldShares> holdShares = holdSharesMapper.selectList(new LambdaQueryWrapper<HoldShares>()
                .ge(HoldShares::getBuyTime, DateUtil.beginOfDay(new Date()))
                .le(HoldShares::getBuyTime, DateUtil.endOfDay(new Date())));
        if (CollectionUtils.isNotEmpty(holdShares)) {
            for (HoldShares holdShare : holdShares) {
                holdShare.setAvailableVolume(holdShare.getNumber());
                holdSharesMapper.updateById(holdShare);
            }
        }
        LambdaUpdateWrapper<Stock> wrapper = new LambdaUpdateWrapper<Stock>()
                .in(Stock::getAutomaticTradingType, AutomaticTradingEnum.PLANK.name(),
                        AutomaticTradingEnum.MANUAL.name(), AutomaticTradingEnum.SUCK.name());
        stockMapper.update(Stock.builder().automaticTradingType(AutomaticTradingEnum.CANCEL.name()).build(), wrapper);
    }

    /**
     * 当前时间是否是交易时间
     * 只判定了时分秒，没有判定非交易日（周末及法定节假日），因为我一般只交易日才会启动项目
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
        List<HoldShares> shares = holdSharesMapper.selectList(new LambdaQueryWrapper<HoldShares>()
                .ge(HoldShares::getBuyTime, DateUtil.beginOfDay(new Date()))
                .ne(HoldShares::getAutomaticTradingType, AutomaticTradingEnum.MANUAL.name()));
        for (HoldShares share : shares) {
            TODAY_COST_MONEY.set((int) (TODAY_COST_MONEY.intValue() + share.getBuyPrice().doubleValue() * share.getNumber()));
            TODAY_BOUGHT_SUCCESS.add(share.getCode());
        }
    }

    class SaleTask implements Runnable {

        private HoldShares holdShare;

        public SaleTask(HoldShares holdShare) {
            this.holdShare = holdShare;
        }

        /**
         * 自动交易的止盈止损策略
         */
        @Override
        public void run() {
            while (isTradeTime() && Objects.nonNull(holdShare) && holdShare.getAvailableVolume() > 0) {
                try {
                    holdShare = holdSharesMapper.selectById(holdShare.getId());
                    StockRealTimePrice stockRealTimePrice = stockProcessor.getStockRealTimePriceByCode(holdShare.getCode());
                    if (stockRealTimePrice.getCurrentPrice() <= holdShare.getStopLossPrice().doubleValue()) {
                        // 触发止损,挂跌停价卖出
                        log.error("{} 触发止损,挂跌停价卖出", holdShare.getName());
                        sale(holdShare, stockRealTimePrice);
                        break;
                    }
                    // 当前盈利
                    holdShare.setProfit(BigDecimal.valueOf((stockRealTimePrice.getCurrentPrice() - holdShare.getBuyPrice().doubleValue())
                            * holdShare.getNumber()));
                    if ((stockRealTimePrice.getHighestPrice().doubleValue() == stockRealTimePrice.getLimitUp().doubleValue()
                            || stockRealTimePrice.isPlank()) && !holdShare.getTodayPlank()) {
                        // 当日首次触板
                        log.error("{} 封板", holdShare.getName());
                        holdShare.setTodayPlank(true);
                    }
                    holdSharesMapper.updateById(holdShare);
                    if (holdShare.getTodayPlank() && !stockRealTimePrice.isPlank()) {
                        // 当日炸板,挂跌停价卖出
                        log.error("{} 炸板,挂跌停价卖出", holdShare.getName());
                        sale(holdShare, stockRealTimePrice);
                        break;
                    }
                    if (holdShare.getAutomaticTradingType().equals(AutomaticTradingEnum.PLANK.name()) ||
                            holdShare.getAutomaticTradingType().equals(AutomaticTradingEnum.SUCK.name())) {
                        // 自定义打板，低吸买入的股票
                        if (holdShare.getTakeProfitPrice().doubleValue() <= stockRealTimePrice.getCurrentPrice()) {
                            sale(holdShare, stockRealTimePrice);
                            log.error("{} 触发止盈,挂跌停价卖出", holdShare.getName());
                            break;
                        }
                    }
                    if (holdShare.getAutomaticTradingType().equals(AutomaticTradingEnum.AUTO_PLANK.name())) {
                        // 自动打板买入的股票
                        if (DateUtil.hour(new Date(), true) > 11 && !stockRealTimePrice.isPlank()) {
                            // 11点前还未涨停,挂跌停价卖出
                            log.error("{} 11点前还未涨停,挂跌停价卖出", holdShare.getName());
                            sale(holdShare, stockRealTimePrice);
                            break;
                        }
                    }
                    Thread.sleep(200);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sale(HoldShares holdShare, StockRealTimePrice stockRealTimePrice) {
        SubmitRequest request = new SubmitRequest(1);
        request.setAmount(holdShare.getAvailableVolume());
        request.setPrice(stockRealTimePrice.getLimitDown());
        request.setStockCode(holdShare.getCode().substring(2, 8));
        request.setZqmc(holdShare.getName());
        request.setTradeType(SubmitRequest.S);
        request.setMarket(StockUtil.getStockMarket(request.getStockCode()));
        TradeResultVo<SubmitResponse> response = tradeApiService.submit(request);
        holdShare.setAvailableVolume(0);
        holdShare.setClearance(true);
        holdShare.setSaleTime(new Date());
        holdShare.setProfit(BigDecimal.valueOf((stockRealTimePrice.getCurrentPrice() - holdShare.getBuyPrice().doubleValue())
                * holdShare.getNumber()));
        holdSharesMapper.updateById(holdShare);
        Stock stock = stockMapper.selectOne(new QueryWrapper<Stock>().eq("name", holdShare.getName()));
        stock.setShareholding(false);
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
            if (stock.getAutomaticTradingType().equals(AutomaticTradingEnum.PLANK.name()) && stockRealTimePrice.isPlank()) {
                // 触发打板下单条件，挂单
                buy(stock, stock.getBuyAmount(), stockRealTimePrice.getLimitUp(), stock.getAutomaticTradingType());
            } else if (stock.getAutomaticTradingType().equals(AutomaticTradingEnum.SUCK.name()) &&
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
    public void buy(Stock stock, int amount, double price, String automaticTradingType) {
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
                    .automaticTradingType(automaticTradingType).buyPrice(BigDecimal.valueOf(price)).build();
            holdSharesMapper.insert(holdShare);
        }
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
