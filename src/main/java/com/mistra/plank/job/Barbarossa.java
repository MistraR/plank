package com.mistra.plank.job;

import static com.mistra.plank.common.config.SystemConstant.W;
import static com.mistra.plank.common.util.StringUtil.collectionToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.collect.Lists;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.common.util.HttpUtil;
import com.mistra.plank.dao.DailyRecordMapper;
import com.mistra.plank.dao.HoldSharesMapper;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.model.dto.StockMainFundSample;
import com.mistra.plank.model.dto.StockRealTimePrice;
import com.mistra.plank.model.entity.Bk;
import com.mistra.plank.model.entity.DailyRecord;
import com.mistra.plank.model.entity.HoldShares;
import com.mistra.plank.model.entity.Stock;
import com.mistra.plank.model.enums.AutomaticTradingEnum;
import com.mistra.plank.service.impl.ScreeningStocks;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;

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
    private final HoldSharesMapper holdSharesMapper;
    private final PlankConfig plankConfig;
    private final ScreeningStocks screeningStocks;
    private final DailyRecordProcessor dailyRecordProcessor;
    private final AnalyzeProcessor analyzePlank;
    @Autowired(required = false)
    private AutomaticPlankTrading automaticPlankTrading;
    public static final ThreadPoolExecutor executorService = new ThreadPoolExecutor(availableProcessors * 2,
            availableProcessors * 2, 100L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(5000), new NamedThreadFactory("Monitor-", false));
    /**
     * 所有股票 key-code value-name
     */
    public static final HashMap<String, String> STOCK_ALL_MAP = new HashMap<>(4096);
    /**
     * 需要重点关注的股票 key-name value-Stock
     */
    public static final ConcurrentHashMap<String, Stock> STOCK_TRACK_MAP = new ConcurrentHashMap<>(32);
    /**
     * 创业板股票
     */
    public static final ConcurrentHashMap<String, Stock> SZ30_STOCKS = new ConcurrentHashMap<>(512);
    /**
     * 主力流入数据
     */
    public static final CopyOnWriteArrayList<StockMainFundSample> MAIN_FUND_DATA = new CopyOnWriteArrayList<>();
    public static final ConcurrentHashMap<String, StockMainFundSample> MAIN_FUND_DATA_MAP =
            new ConcurrentHashMap<>(4096);
    /**
     * 是否开启监控中
     */
    private final AtomicBoolean monitoring = new AtomicBoolean(false);

    public Barbarossa(StockMapper stockMapper, StockProcessor stockProcessor, DailyRecordMapper dailyRecordMapper,
                      HoldSharesMapper holdSharesMapper, PlankConfig plankConfig, ScreeningStocks screeningStocks,
                      DailyRecordProcessor dailyRecordProcessor, AnalyzeProcessor analyzePlank) {
        this.stockMapper = stockMapper;
        this.stockProcessor = stockProcessor;
        this.dailyRecordMapper = dailyRecordMapper;
        this.holdSharesMapper = holdSharesMapper;
        this.plankConfig = plankConfig;
        this.screeningStocks = screeningStocks;
        this.dailyRecordProcessor = dailyRecordProcessor;
        this.analyzePlank = analyzePlank;
    }

    /**
     * 启动前端服务
     * cd ./stock-web
     * npm start
     */
    @Override
    public void run(String... args) {
        monitor();
    }

    /**
     * 初始化股票基本数据
     */
    private void updateStockCache() {
        List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>()
                // 默认过滤掉了北交所,科创板,ST
                .notLike("name", "%ST%").notLike("code", "%688%")
                .notLike("name", "%st%").notLike("name", "%A%").notLike("name", "%N%")
                .notLike("name", "%U%").notLike("name", "%W%").notLike("code", "%BJ%"));
        STOCK_TRACK_MAP.clear();
        stocks.forEach(e -> {
            if ((e.getShareholding() || e.getTrack())) {
                STOCK_TRACK_MAP.put(e.getName(), e);
            }
            if (e.getCode().startsWith("SZ30")) {
                SZ30_STOCKS.put(e.getCode(), e);
            }
            STOCK_ALL_MAP.put(e.getCode(), e.getName());
        });
    }

    /**
     * 每10秒更新一次版块涨跌幅
     */
    @Scheduled(cron = "*/10 * * * * ?")
    private void updateBkCache() {
        if (AutomaticTrading.isTradeTime()) {
            // 更新行业版块，概念版块涨幅信息
            stockProcessor.updateBk();
            stockProcessor.updateTop5IncreaseRateBk();
        }
    }

    /**
     * 实时监测数据 显示股票实时涨跌幅度，最高，最低价格，主力流入
     * 想要监测哪些股票需要手动在数据库stock表更改track字段为true
     */
    @Scheduled(cron = "0 */2 * * * ?")
    public void monitor() {
        if (plankConfig.getEnableMonitor() && AutomaticTrading.isTradeTime() && !monitoring.get() && STOCK_TRACK_MAP.size() > 0) {
            monitoring.set(true);
            executorService.submit(this::monitorStock);
            executorService.submit(this::queryMainFundData);
        }
        updateStockCache();
    }

    /**
     * 复盘就会发现,大的亏损都是不遵守卖出原则导致的,对分时图的下跌趋势存在幻想,幻想它会扭转下跌趋势
     * 当然,不排除会在你卖出之后走强,但是首要原则是防止亏损,因为由赚到亏是非常伤害心态的
     *
     * 有4条硬性止损原则必须执行,抛弃幻想
     * 从盈利到跌破成本 | 跌破止损位7% | 跌破MA10 | 当日亏损达到总仓位-2%    核
     *
     * 止盈原则
     * 可以分批止盈 | 大幅脉冲无量可以清 | 脉冲之后跌破均线反弹无力可以清
     *
     * 只做主升,不做震荡和下跌趋势
     * MA5均线之上的龙头可逢低参与
     * 2板梯队对比选强,竞价上底仓,上板加
     */
    private void monitorStock() {
        try {
            List<StockRealTimePrice> realTimePrices = new ArrayList<>();
            while (AutomaticTrading.isTradeTime()) {
                List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                        .in(Stock::getName, STOCK_TRACK_MAP.keySet()));
                for (Stock stock : stocks) {
                    // 默认把MA10作为建仓基准价格
                    int purchaseType = Objects.isNull(stock.getPurchaseType()) || stock.getPurchaseType() == 0 ? 10
                            : stock.getPurchaseType();
                    List<DailyRecord> dailyRecords = dailyRecordMapper.selectList(new LambdaQueryWrapper<DailyRecord>()
                            .eq(DailyRecord::getCode, stock.getCode())
                            .ge(DailyRecord::getDate, DateUtils.addDays(new Date(), -purchaseType * 3))
                            .orderByDesc(DailyRecord::getDate));
                    if (dailyRecords.size() < purchaseType) {
                        log.error("{}的交易数据不完整,不足{}个交易日数据,请先爬取交易数据", stock.getCode(), stock.getPurchaseType());
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
                    stockRealTimePrice.setMainFund(MAIN_FUND_DATA_MAP.containsKey(stock.getName())
                            ? MAIN_FUND_DATA_MAP.get(stock.getName()).getF62() / W : 0);
                    stockRealTimePrice.setPurchasePrice(maPrice);
                    stockRealTimePrice.setPurchaseRate((int) (purchaseRate * 100));
                    realTimePrices.add(stockRealTimePrice);
                }
                Collections.sort(realTimePrices);
                System.out.println("\n\n\n");
                log.error("------------------------ 主力净流入Top10 --------------------------");
                List<StockMainFundSample> topTen = new ArrayList<>();
                for (int i = 0; i < Math.min(MAIN_FUND_DATA.size(), 10); i++) {
                    topTen.add(MAIN_FUND_DATA.get(i));
                }
                log.warn(collectionToString(topTen.stream().map(e -> e.getF14() + e.getF3()).collect(Collectors.toList())));
                log.error("------------------------- 板块涨幅>2Top5 --------------------------");
                ArrayList<Bk> bks = Lists.newArrayList(StockProcessor.TOP5_BK.values());
                Collections.sort(bks);
                log.warn(collectionToString(bks.stream().map(e -> e.getName() + ":" + e.getIncreaseRate()).collect(Collectors.toList())));
                List<StockRealTimePrice> shareholding = realTimePrices.stream().filter(e -> STOCK_TRACK_MAP.containsKey(e.getName()) &&
                        STOCK_TRACK_MAP.get(e.getName()).getShareholding()).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(shareholding)) {
                    log.error("------------------------------- 持仓 ------------------------------");
                    shareholding.forEach(this::print);
                }
                realTimePrices.removeIf(e -> STOCK_TRACK_MAP.containsKey(e.getName()) && STOCK_TRACK_MAP.get(e.getName()).getShareholding());
                List<StockRealTimePrice> stockRealTimePrices = realTimePrices.stream().filter(e ->
                        e.getPurchaseRate() >= -2).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(stockRealTimePrices)) {
                    log.error("------------------------------ Track ------------------------------");
                    stockRealTimePrices.forEach(this::print);
                }
                List<HoldShares> buyStocks = holdSharesMapper.selectList(new LambdaQueryWrapper<HoldShares>()
                        .ge(HoldShares::getBuyTime, DateUtil.beginOfDay(new Date()))
                        .le(HoldShares::getBuyTime, DateUtil.endOfDay(new Date()))
                        .ne(HoldShares::getAutomaticTradingType, AutomaticTradingEnum.MANUAL.name()));
                if (CollectionUtils.isNotEmpty(buyStocks)) {
                    log.error("----------------------- 自动打板,排单金额:{} -----------------------",
                            AutomaticTrading.TODAY_COST_MONEY.intValue());
                    log.warn("{}", collectionToString(buyStocks.stream().map(HoldShares::getName).collect(Collectors.toSet())));
                    if (plankConfig.getAutomaticPlankTrading() && automaticPlankTrading.openAutoPlank()) {
                        log.warn("打板监测:{}", collectionToString(AutomaticPlankTrading.PLANK_MONITOR.values().stream()
                                .map(Stock::getName).collect(Collectors.toList())));
                    }
                }
                if (CollectionUtils.isNotEmpty(AutomaticTrading.UNDER_MONITORING.values())) {
                    log.error("--------------------------- 自定义交易监测 --------------------------");
                    log.warn("{}", collectionToString(AutomaticTrading.UNDER_MONITORING.values().stream()
                            .map(Stock::getName).collect(Collectors.toList())));
                }
                realTimePrices.clear();
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            monitoring.set(false);
        }
    }

    private void print(StockRealTimePrice stockRealTimePrices) {
        if (stockRealTimePrices.getIncreaseRate() > 0) {
            Barbarossa.log.error(convertLog(stockRealTimePrices));
        } else {
            Barbarossa.log.warn(convertLog(stockRealTimePrices));
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
                ArrayList<StockMainFundSample> tmpList = new ArrayList<>();
                array.forEach(e -> {
                    try {
                        StockMainFundSample mainFundSample = JSONObject.parseObject(e.toString(), StockMainFundSample.class);
                        tmpList.add(mainFundSample);
                        MAIN_FUND_DATA_MAP.put(mainFundSample.getF14(), mainFundSample);
                    } catch (JSONException ignored) {
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                });
                MAIN_FUND_DATA.clear();
                MAIN_FUND_DATA.addAll(tmpList.stream().filter(e -> e.getF62() > 100000000).collect(Collectors.toList()));
                Thread.sleep(3000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 15点后读取当日交易数据
     */
    @Scheduled(cron = "0 1 15 * * ?")
    private void analyzeData() {
        try {
            CountDownLatch countDownLatch = new CountDownLatch(Barbarossa.STOCK_ALL_MAP.size());
            dailyRecordProcessor.run(Barbarossa.STOCK_ALL_MAP, countDownLatch);
            this.resetStockData();
            countDownLatch.await();
            List<List<String>> partition = Lists.partition(Lists.newArrayList(Barbarossa.STOCK_ALL_MAP.keySet()), 300);
            for (List<String> list : partition) {
                // 更新每支股票的成交额
                executorService.submit(() -> stockProcessor.run(list));
            }
            log.warn("每日涨跌明细、成交额、MA5、MA10、MA20更新完成");
            executorService.submit(stockProcessor::updateStockBkInfo);
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
     * 重置stock表,持仓表数据
     */
    private void resetStockData() {
        stockMapper.update(Stock.builder().plankNumber(0).autoPlank(false).cancelPlank(false).automaticTradingType(AutomaticTradingEnum.CANCEL.name()).suckTriggerPrice(new BigDecimal(0)).buyAmount(0).build(), new LambdaUpdateWrapper<>());
        holdSharesMapper.update(HoldShares.builder().todayPlank(false).build(), new LambdaUpdateWrapper<HoldShares>().eq(HoldShares::getClearance,
                false));
    }

    private String convertLog(StockRealTimePrice realTimePrice) {
        return realTimePrice.getName() + (realTimePrice.getName().length() == 3 ? "  " : "") +
                ">高:" + realTimePrice.getHighestPrice() + "|现:" + realTimePrice.getCurrentPrice() +
                "|低:" + realTimePrice.getLowestPrice() + "|" + realTimePrice.getIncreaseRate() + "%";
    }

}
