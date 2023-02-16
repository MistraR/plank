package com.mistra.plank.job;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.NamedThreadFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.common.util.HttpUtil;
import com.mistra.plank.config.SystemConstant;
import com.mistra.plank.dao.*;
import com.mistra.plank.model.dto.StockMainFundSample;
import com.mistra.plank.model.dto.StockRealTimePrice;
import com.mistra.plank.model.entity.DailyRecord;
import com.mistra.plank.model.entity.HoldShares;
import com.mistra.plank.model.entity.Stock;
import com.mistra.plank.service.Plank;
import com.mistra.plank.service.impl.ScreeningStocks;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.mistra.plank.common.util.StringUtil.collectionToString;

/**
 * 涨停先锋
 *
 * @author mistra@future.com
 * @date 2021/11/19
 */
@Slf4j
@Component
public class Barbarossa implements CommandLineRunner {

    private static final int availableProcessors = Runtime.getRuntime().availableProcessors();
    private final StockMapper stockMapper;
    private final StockProcessor stockProcessor;
    private final DailyRecordMapper dailyRecordMapper;
    private final ClearanceMapper clearanceMapper;
    private final TradeRecordMapper tradeRecordMapper;
    private final HoldSharesMapper holdSharesMapper;
    private final Plank plank;
    private final PlankConfig plankConfig;
    private final ScreeningStocks screeningStocks;
    private final DailyRecordProcessor dailyRecordProcessor;
    private final AnalyzeProcessor analyzePlank;
    public static final ThreadPoolExecutor executorService = new ThreadPoolExecutor(availableProcessors,
            availableProcessors * 2, 100L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(8192), new NamedThreadFactory("T", false));
    /**
     * 所有股票 key-code value-name
     */
    public static final HashMap<String, String> STOCK_MAP_ALL = new HashMap<>(4096);
    /**
     * 需要监控关注的机构趋势票 key-name value-Stock
     */
    public static final HashMap<String, Stock> STOCK_MAP_TRACK = new HashMap<>(32);
    /**
     * 昨日成交额大于3亿的股票
     */
    public static final HashMap<String, Stock> STOCK_MAP_GE_3E = new HashMap<>(2048);
    /**
     * 所有股票 name
     */
    public static final HashSet<String> STOCK_NAME_SET_ALL = new HashSet<>();
    public static final CopyOnWriteArrayList<StockMainFundSample> mainFundData = new CopyOnWriteArrayList<>();
    public static final CopyOnWriteArrayList<StockMainFundSample> mainFundDataAll = new CopyOnWriteArrayList<>();
    public static final ConcurrentHashMap<String, StockMainFundSample> mainFundDataMap = new ConcurrentHashMap<>(64);
    public static final ConcurrentHashMap<String, StockMainFundSample> mainFundDataAllMap =
            new ConcurrentHashMap<>(4096);
    /**
     * 模拟交易总金额
     */
    public static BigDecimal BALANCE = new BigDecimal(100 * SystemConstant.W);
    /**
     * 模拟交易可用金额
     */
    public static BigDecimal BALANCE_AVAILABLE = new BigDecimal(100 * SystemConstant.W);
    /**
     * 是否开启监控中
     */
    private final AtomicBoolean monitoring = new AtomicBoolean(false);

    public Barbarossa(StockMapper stockMapper, StockProcessor stockProcessor, DailyRecordMapper dailyRecordMapper,
                      ClearanceMapper clearanceMapper, TradeRecordMapper tradeRecordMapper, HoldSharesMapper holdSharesMapper,
                      Plank plank, PlankConfig plankConfig, ScreeningStocks screeningStocks,
                      DailyRecordProcessor dailyRecordProcessor, AnalyzeProcessor analyzePlank) {
        this.stockMapper = stockMapper;
        this.stockProcessor = stockProcessor;
        this.dailyRecordMapper = dailyRecordMapper;
        this.clearanceMapper = clearanceMapper;
        this.tradeRecordMapper = tradeRecordMapper;
        this.holdSharesMapper = holdSharesMapper;
        this.plank = plank;
        this.plankConfig = plankConfig;
        this.screeningStocks = screeningStocks;
        this.dailyRecordProcessor = dailyRecordProcessor;
        this.analyzePlank = analyzePlank;
    }

    @Override
    public void run(String... args) {
        List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>()
                // 默认过滤掉了北交所，科创板，ST
                .notLike("name", "%ST%").notLike("code", "%688%")
                .notLike("name", "%st%").notLike("name", "%A%").notLike("name", "%N%")
                .notLike("name", "%U%").notLike("name", "%W%").notLike("code", "%BJ%"));
        stocks.forEach(e -> {
            if ((e.getShareholding() || e.getTrack())) {
                STOCK_MAP_TRACK.put(e.getName(), e);
            } else if (e.getTransactionAmount().intValue() > plankConfig.getStockTurnoverFilter()
                    && (Objects.isNull(e.getPlankNumber()) || e.getPlankNumber() <= 3)
                    && (Objects.isNull(e.getBuyTime()) || !DateUtils.isSameDay(new Date(), e.getBuyTime()))) {
                // 过滤掉昨日连板以及成交额小于3亿的股票
                STOCK_MAP_GE_3E.put(e.getCode(), e);
            }
            STOCK_MAP_ALL.put(e.getCode(), e.getName());
        });
        log.warn("一共加载[{}]支股票", stocks.size());
        log.warn("一共加载[{}]支自动打板监测股票", STOCK_MAP_GE_3E.size());
        STOCK_NAME_SET_ALL.addAll(STOCK_MAP_ALL.keySet());
    }

    @Scheduled(cron = "0 1 15 * * ?")
    private void analyzeData() {
        try {
            // 15点后读取当日交易数据
            dailyRecordProcessor.run(Barbarossa.STOCK_MAP_ALL);
            Thread.sleep(10 * 60 * 1000);
            // 更新每只股票收盘价，当日成交量，MA5 MA10 MA20
            executorService.submit(stockProcessor::run);
            // 更新 外资+基金 持仓 只更新到最新季度报告的汇总表上 基金季报有滞后性，外资持仓则是实时计算，每天更新的
            executorService.submit(stockProcessor::updateForeignFundShareholding);
            executorService.submit(() -> {
                // 分析连板数据
                analyzePlank.analyzePlank();
                // 分析主力流入数据
                analyzePlank.analyzeMainFund();
                // 分析日k均线多头排列的股票
                screeningStocks.movingAverageRise();
                // 分析上升趋势的股票，周k均线多头排列
                screeningStocks.upwardTrend();
                // 分析爆量回踩
                screeningStocks.explosiveVolumeBack();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 此方法主要用来预警接近建仓价的股票
     * 实时监测数据 显示股票实时涨跌幅度，最高，最低价格，主力流入
     * 想要监测哪些股票需要手动在数据库stock表更改track字段为true
     * 我一般会选择趋势股或赛道股，所以默认把MA10作为建仓基准价格，可以手动修改stock.purchase_type字段来设置，5-则以MA5为基准价格,最多MA20
     * 股价除权之后需要重新爬取交易数据，算均价就不准了
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void monitor() {
        if (AutomaticTrading.isTradeTime() && plankConfig.getEnableMonitor() && !monitoring.get()) {
            monitoring.set(true);
            executorService.submit(this::monitorStock);
            executorService.submit(this::queryMainFundData);
        }
    }

    private void monitorStock() {
        try {
            List<StockRealTimePrice> realTimePrices = new ArrayList<>();
            while (AutomaticTrading.isTradeTime()) {
                List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                        .in(Stock::getName, STOCK_MAP_TRACK.keySet()));
                for (Stock stock : stocks) {
                    // 默认把MA10作为建仓基准价格
                    int purchaseType = Objects.isNull(stock.getPurchaseType()) || stock.getPurchaseType() == 0 ? 10
                            : stock.getPurchaseType();
                    List<DailyRecord> dailyRecords =
                            dailyRecordMapper.selectList(new LambdaQueryWrapper<DailyRecord>().eq(DailyRecord::getCode, stock.getCode())
                                    .ge(DailyRecord::getDate, DateUtils.addDays(new Date(), -purchaseType * 3))
                                    .orderByDesc(DailyRecord::getDate));
                    if (dailyRecords.size() < purchaseType) {
                        log.error("{}的交易数据不完整，不够{}个交易日数据！请先爬取交易数据！", stock.getCode(), stock.getPurchaseType());
                        continue;
                    }
                    StockRealTimePrice stockRealTimePrice = stockProcessor.getStockRealTimePriceByCode(stock.getCode());
                    double v = stockRealTimePrice.getCurrentPrice();
                    List<BigDecimal> collect = dailyRecords.subList(0, purchaseType - 1).stream()
                            .map(DailyRecord::getClosePrice).collect(Collectors.toList());
                    collect.add(new BigDecimal(v).setScale(2, RoundingMode.HALF_UP));
                    double ma = collect.stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue));
                    // 如果手动设置了purchasePrice，则以stock.purchasePrice 和均线价格 2个当中更低的价格为基准价
                    if (Objects.nonNull(stock.getPurchasePrice()) && stock.getPurchasePrice().doubleValue() > 0) {
                        ma = Math.min(stock.getPurchasePrice().doubleValue(), ma);
                    }
                    BigDecimal maPrice = new BigDecimal(ma).setScale(2, RoundingMode.HALF_UP);
                    double purchaseRate = (double) Math.round(((maPrice.doubleValue() - v) / v) * 100) / 100;
                    stockRealTimePrice.setName(stock.getName());
                    stockRealTimePrice.setMainFund(mainFundDataMap.containsKey(stock.getName())
                            ? mainFundDataMap.get(stock.getName()).getF62() / SystemConstant.W : 0);
                    stockRealTimePrice.setPurchasePrice(maPrice);
                    stockRealTimePrice.setPurchaseRate((int) (purchaseRate * 100));
                    realTimePrices.add(stockRealTimePrice);
                }
                Collections.sort(realTimePrices);
                System.out.println("\n\n\n");
                log.error("------------------------ 主力净流入Top10 -------------------------");
                List<StockMainFundSample> topTen = new ArrayList<>();
                for (int i = 0; i < Math.min(mainFundDataAll.size(), 10); i++) {
                    topTen.add(mainFundDataAll.get(i));
                }
                log.warn(collectionToString(
                        topTen.stream().map(e -> e.getF14() + "[" + e.getF62() / SystemConstant.W / SystemConstant.W + "亿]" + e.getF3() + "%")
                                .collect(Collectors.toList())));
                log.error("------------------------------ 持仓 -----------------------------");
                for (StockRealTimePrice realTimePrice : realTimePrices) {
                    if (STOCK_MAP_TRACK.get(realTimePrice.getName()).getShareholding()) {
                        if (realTimePrice.getIncreaseRate() > 0) {
                            Barbarossa.log.error(convertLog(realTimePrice));
                        } else {
                            Barbarossa.log.warn(convertLog(realTimePrice));
                        }
                    }
                }
                realTimePrices.removeIf(e -> STOCK_MAP_TRACK.get(e.getName()).getShareholding());
                log.error("------------------------------ 建仓 -----------------------------");
                for (StockRealTimePrice realTimePrice : realTimePrices) {
                    if (realTimePrice.getPurchaseRate() >= -2) {
                        Barbarossa.log.warn(convertLog(realTimePrice));
                    }
                }
                log.error("---------------------------- 打板排单 ----------------------------");
                List<HoldShares> buyStocks = holdSharesMapper.selectList(new LambdaQueryWrapper<HoldShares>()
                        .ge(HoldShares::getBuyTime, DateUtil.beginOfDay(new Date()))
                        .le(HoldShares::getBuyTime, DateUtil.endOfDay(new Date())));
                log.warn("{}", buyStocks.stream().map(HoldShares::getName).collect(Collectors.toSet()));
                log.error("---------------------------- 打板监测 ----------------------------");
                if (CollectionUtils.isNotEmpty(AutomaticTrading.runningMap.values())) {
                    log.warn("{}", collectionToString(AutomaticTrading.runningMap.values().stream()
                            .map(Stock::getName).collect(Collectors.toList())));
                }
                realTimePrices.clear();
                Thread.sleep(3000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            monitoring.set(false);
        }
    }

    /**
     * 查询主力实时流入数据
     */
    private void queryMainFundData() {
        while (AutomaticTrading.isTradeTime()) {
            try {
                String body = HttpUtil.getHttpGetResponseString(plankConfig.getMainFundUrl(), null);
                JSONArray array = JSON.parseObject(body).getJSONObject("data").getJSONArray("diff");
                List<StockMainFundSample> tmpList = new ArrayList<>();
                array.parallelStream().forEach(e -> {
                    try {
                        StockMainFundSample mainFundSample = JSONObject.parseObject(e.toString(), StockMainFundSample.class);
                        tmpList.add(mainFundSample);
                        mainFundDataAllMap.put(mainFundSample.getF14(), mainFundSample);
                        if (STOCK_MAP_TRACK.containsKey(mainFundSample.getF14())) {
                            mainFundDataMap.put(mainFundSample.getF14(), mainFundSample);
                        }
                    } catch (JSONException ignored) {
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                });
                List<StockMainFundSample> result = tmpList.stream().filter(e -> e != null &&
                                STOCK_NAME_SET_ALL.contains(e.getF14()) && e.getF62() != null)
                        .sorted().collect(Collectors.toList());
                mainFundDataAll.clear();
                mainFundDataAll.addAll(result.stream().filter(e -> e.getF62() > 100000000).collect(Collectors.toList()));
                mainFundData.clear();
                mainFundData.addAll(
                        result.stream().filter(e -> STOCK_MAP_TRACK.containsKey(e.getF14())).collect(Collectors.toList()));
                Thread.sleep(3000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        monitoring.set(false);
    }

    private String convertLog(StockRealTimePrice realTimePrice) {
        return new StringBuilder().append(realTimePrice.getName())
                .append((realTimePrice.getName().length() == 3 ? "  " : ""))
                .append("[高:").append(realTimePrice.getHighestPrice())
                .append("|现:").append(realTimePrice.getCurrentPrice())
                .append("|低:").append(realTimePrice.getLowestPrice())
                .append("|差距:").append(realTimePrice.getPurchaseRate())
                .append("%|涨幅:").append(realTimePrice.getIncreaseRate())
                .append("|主力:").append(realTimePrice.getMainFund()).append("万]").toString();
    }

    /**
     * 以历史数据为样本，根据配置的买入，卖出，分仓策略自动交易
     */
    public void barbarossa(Integer fundsPart, Long beginDay) {
        // 清除老数据
        holdSharesMapper.delete(new QueryWrapper<>());
        clearanceMapper.delete(new QueryWrapper<>());
        tradeRecordMapper.delete(new QueryWrapper<>());
        BALANCE = new BigDecimal(100 * SystemConstant.W);
        BALANCE_AVAILABLE = new BigDecimal(100 * SystemConstant.W);
        Date date = new Date(beginDay);
        DateUtils.setHours(date, 0);
        DateUtils.setMinutes(date, 0);
        DateUtils.setSeconds(date, 0);
        DateUtils.setMilliseconds(date, 0);
        do {
            this.barbarossa(date, fundsPart);
            date = DateUtils.addDays(date, 1);
        } while (date.getTime() < System.currentTimeMillis());
    }

    private void barbarossa(Date date, Integer fundsPart) {
        int week = DateUtil.dayOfWeek(date);
        if (week < 7 && week > 1) {
            // 工作日
            List<Stock> stocks = plank.checkStock(date);
            if (CollectionUtils.isNotEmpty(stocks) && BALANCE_AVAILABLE.intValue() > SystemConstant.W) {
                plank.buyStock(stocks, date, fundsPart);
            }
            plank.sellStock(date);
        }
    }
}
