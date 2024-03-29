package com.mistra.plank.job;

import static com.mistra.plank.common.config.SystemConstant.W;
import static com.mistra.plank.common.util.StockUtil.isSZ30;
import static com.mistra.plank.common.util.StringUtil.collectionToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
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
import com.mistra.plank.dao.BkMapper;
import com.mistra.plank.dao.DailyIndexMapper;
import com.mistra.plank.dao.DailyRecordMapper;
import com.mistra.plank.dao.HoldSharesMapper;
import com.mistra.plank.dao.StockInfoDao;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.model.dto.StockMainFundSample;
import com.mistra.plank.model.dto.StockRealTimePrice;
import com.mistra.plank.model.entity.Bk;
import com.mistra.plank.model.entity.DailyIndex;
import com.mistra.plank.model.entity.DailyRecord;
import com.mistra.plank.model.entity.HoldShares;
import com.mistra.plank.model.entity.Stock;
import com.mistra.plank.model.entity.StockInfo;
import com.mistra.plank.model.enums.AutomaticTradingEnum;
import com.mistra.plank.service.impl.ScreeningStocks;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Mistra @ Version: 1.0
 * @ Time: 2021/11/18 22:09
 * @ Description: 涨停
 * @ Copyright (c) Mistra,All Rights Reserved
 * @ Github: https://github.com/MistraR
 * @ CSDN: https://blog.csdn.net/axela30w
 */
@Slf4j
@Component
public class Barbarossa implements CommandLineRunner {

    private static final int availableProcessors = Runtime.getRuntime().availableProcessors();
    private final BkMapper bkMapper;
    private final StockMapper stockMapper;
    private final HoldSharesMapper holdSharesMapper;
    private final DailyRecordMapper dailyRecordMapper;
    private final PlankConfig plankConfig;
    private final AnalyzeProcessor analyzePlank;
    private final StockProcessor stockProcessor;
    private final ScreeningStocks screeningStocks;
    private final DailyRecordProcessor dailyRecordProcessor;
    private final AutomaticPlankTrading automaticPlankTrading;
    private final StockInfoDao stockInfoDao;
    private final DailyIndexMapper dailyIndexMapper;
    public static final ThreadPoolExecutor executorService = new ThreadPoolExecutor(availableProcessors * 2,
            availableProcessors * 2, 100L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(5000), new NamedThreadFactory("行情-", false));
    /**
     * 所有股票 key-code value-name
     */
    public static final HashMap<String, String> ALL_STOCK_MAP = new HashMap<>(4096);
    /**
     * 需要重点关注的股票 key-name value-Stock
     */
    public static final ConcurrentHashMap<String, Stock> TRACK_STOCK_MAP = new ConcurrentHashMap<>(32);
    /**
     * 成交额>3亿的创业板股票
     */
    public static final ConcurrentHashMap<String, Stock> SZ30_STOCK_MAP = new ConcurrentHashMap<>(512);
    /**
     * 市值1000亿以内,成交额>3亿的10cm股票
     */
    public static final ConcurrentHashMap<String, Stock> SH10_STOCK_MAP = new ConcurrentHashMap<>(1024);
    /**
     * 主力流入数据
     */
    public static final CopyOnWriteArrayList<StockMainFundSample> MAIN_FUND_DATA = new CopyOnWriteArrayList<>();
    public static final ConcurrentHashMap<String, StockMainFundSample> MAIN_FUND_DATA_MAP = new ConcurrentHashMap<>(4096);
    /**
     * 当日涨幅top5版块
     */
    public static final ConcurrentHashMap<String, Bk> TOP5_BK = new ConcurrentHashMap<>(8);
    /**
     * 是否开启监控中
     */
    private final AtomicBoolean monitoring = new AtomicBoolean(false);

    public Barbarossa(StockMapper stockMapper, BkMapper bkMapper, StockProcessor stockProcessor, DailyRecordMapper dailyRecordMapper,
                      HoldSharesMapper holdSharesMapper, PlankConfig plankConfig, ScreeningStocks screeningStocks,
                      DailyRecordProcessor dailyRecordProcessor, AnalyzeProcessor analyzePlank,
                      AutomaticPlankTrading automaticPlankTrading, StockInfoDao stockInfoDao, DailyIndexMapper dailyIndexMapper) {
        this.stockMapper = stockMapper;
        this.bkMapper = bkMapper;
        this.stockProcessor = stockProcessor;
        this.dailyRecordMapper = dailyRecordMapper;
        this.holdSharesMapper = holdSharesMapper;
        this.plankConfig = plankConfig;
        this.screeningStocks = screeningStocks;
        this.dailyRecordProcessor = dailyRecordProcessor;
        this.analyzePlank = analyzePlank;
        this.automaticPlankTrading = automaticPlankTrading;
        this.stockInfoDao = stockInfoDao;
        this.dailyIndexMapper = dailyIndexMapper;
    }

    /**
     * 启动前端服务
     * cd ./stock-web
     * npm start
     */
    @Override
    public void run(String... args) {
        List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>()
                // 默认过滤掉了北交所,科创板,ST
                .notLike("name", "%ST%").notLike("code", "%688%").notLike("name", "%退%")
                .notLike("name", "%st%").notLike("name", "%A%").notLike("name", "%N%")
                .notLike("name", "%U%").notLike("name", "%W%").notLike("code", "%BJ%"));
        // 查询自动打板的板块,属于这些板块的股票才会盯盘
        List<Bk> bks = bkMapper.selectList(new LambdaQueryWrapper<Bk>().eq(Bk::getAutoPlank, true));
        Set<String> BK = bks.stream().map(Bk::getBk).collect(Collectors.toSet());
        stocks.forEach(e -> {
            if ((e.getShareholding() || e.getTrack())) {
                TRACK_STOCK_MAP.put(e.getName(), e);
            }
            // 手动排除掉(CANCEL_AUTO_PLANK)的股票不参与打板，havingBk()只打某些板块的票
            if (plankConfig.getAutomaticPlankTrading() &&
                    !e.getAutomaticTradingType().equals(AutomaticTradingEnum.CANCEL_AUTO_PLANK.name()) &&
                    plankConfig.getAutomaticPlankLevel().contains(e.getPlankNumber()) &&
                    havingBk(e.getClassification(), BK)) {
                if (e.getTransactionAmount().longValue() > 300000000L && e.getMarketValue() < 100000000000L && e.getMarketValue() > 1000000000L) {
                    if (isSZ30(e.getCode())) {
                        SZ30_STOCK_MAP.put(e.getCode(), e);
                    } else {
                        SH10_STOCK_MAP.put(e.getCode(), e);
                    }
                }
            }
            ALL_STOCK_MAP.put(e.getCode(), e.getName());
        });
        log.info("盯板 20CM:{}支 10CM:{}支", SZ30_STOCK_MAP.size(), SH10_STOCK_MAP.size());
        log.info("盯板 20CM:[{}]", collectionToString(SZ30_STOCK_MAP.values().stream().map(Stock::getName).collect(Collectors.toList())));
        log.info("盯板 10CM:[{}]", collectionToString(SH10_STOCK_MAP.values().stream().map(Stock::getName).collect(Collectors.toList())));
        bkMapper.update(Bk.builder().increaseRate(new BigDecimal(0)).build(), new LambdaUpdateWrapper<Bk>());
        updateBkCache();
        monitor();
    }

    private boolean havingBk(String bk, Set<String> BK) {
        if (StringUtils.isBlank(bk)) {
            return false;
        }
        String[] split = bk.split(",");
        for (String s : split) {
            if (BK.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 更新盯板一级缓存
     */
    @Scheduled(cron = "0 */1 * * * ?")
    private void updatePlankStockCache() {
        if (!automaticPlankTrading.openAutoPlank()) {
            return;
        }
        long begin = System.currentTimeMillis();
        Set<String> tradedStock = automaticPlankTrading.selectTodayTradedStock();
        Barbarossa.SZ30_STOCK_MAP.keySet().parallelStream().filter(code -> !AutomaticPlankTrading.PLANK_LEVEL1_CACHE.containsKey(code)
                && !AutomaticPlankTrading.PLANKING_CACHE.containsKey(code) && !tradedStock.contains(code)).forEach(e -> {
            try {
                StockRealTimePrice stockRealTimePriceByCode = stockProcessor.getStockRealTimePriceByCode(e);
                if (Objects.isNull(stockRealTimePriceByCode) || Objects.isNull(stockRealTimePriceByCode.getIncreaseRate())) {
                    return;
                }
                // 自动打板是否开启只打当日涨幅top5的板块
                if (plankConfig.getAutomaticPlankTopBk() && !havingBk(Barbarossa.SZ30_STOCK_MAP.get(e).getClassification(), TOP5_BK.keySet())) {
                    return;
                }
                if (stockRealTimePriceByCode.getIncreaseRate() > 17) {
                    // 涨幅>17直接新起线程盯板
                    automaticPlankTrading.plank(Barbarossa.SZ30_STOCK_MAP.get(e));
                } else if (stockRealTimePriceByCode.getIncreaseRate() > 10) {
                    // 涨幅>10放入一级缓存
                    AutomaticPlankTrading.PLANK_LEVEL1_CACHE.put(e, Barbarossa.SZ30_STOCK_MAP.get(e));
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
        Barbarossa.SH10_STOCK_MAP.keySet().parallelStream().filter(code -> !AutomaticPlankTrading.PLANK_LEVEL1_CACHE.containsKey(code) &&
                !AutomaticPlankTrading.PLANKING_CACHE.containsKey(code) && !tradedStock.contains(code)).forEach(e -> {
            try {
                StockRealTimePrice stockRealTimePriceByCode = stockProcessor.getStockRealTimePriceByCode(e);
                if (Objects.isNull(stockRealTimePriceByCode) || Objects.isNull(stockRealTimePriceByCode.getIncreaseRate())) {
                    return;
                }
                // 自动打板是否开启只打当日涨幅top5的板块
                if (plankConfig.getAutomaticPlankTopBk() && !havingBk(Barbarossa.SH10_STOCK_MAP.get(e).getClassification(), TOP5_BK.keySet())) {
                    return;
                }
                if (stockRealTimePriceByCode.getIncreaseRate() > 7.5) {
                    // 涨幅>7.5直接新起线程盯板
                    automaticPlankTrading.plank(Barbarossa.SH10_STOCK_MAP.get(e));
                } else if (stockRealTimePriceByCode.getIncreaseRate() > 5.5) {
                    // 涨幅>5.5放入一级缓存
                    AutomaticPlankTrading.PLANK_LEVEL1_CACHE.put(e, Barbarossa.SH10_STOCK_MAP.get(e));
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
        log.info("更新盯盘一级缓存耗时: {} 秒,一共 {} 支:{}", (System.currentTimeMillis() - begin) / 1000, AutomaticPlankTrading.PLANK_LEVEL1_CACHE.size(),
                collectionToString(AutomaticPlankTrading.PLANK_LEVEL1_CACHE.values().stream().map(Stock::getName).collect(Collectors.toList())));
    }

    /**
     * 更新版块涨跌幅
     */
    @Scheduled(cron = "*/20 * * * * ?")
    private void updateBkCache() {
        if (AutomaticTrading.isTradeTime()) {
            // 更新行业版块，概念版块涨幅信息
            stockProcessor.updateBk();
            List<Bk> bks = bkMapper.selectList(new LambdaQueryWrapper<Bk>().eq(Bk::getIgnoreUpdate, false)
                    .orderByDesc(Bk::getIncreaseRate).last("limit 0,5"));
            TOP5_BK.clear();
            // 查询涨幅前5的版块,并且板块涨幅要大于2,说明行情比较好,普跌行情就不参与了
            bks.stream().filter(e -> e.getIncreaseRate().doubleValue() > 2).forEach(e -> TOP5_BK.put(e.getBk(), e));
        }
    }

    /**
     * 实时监测数据 显示股票实时涨跌幅度，最高，最低价格，主力流入
     * 想要监测哪些股票需要手动在数据库stock表更改track字段为true
     */
    @Scheduled(cron = "0 */2 * * * ?")
    public void monitor() {
        if (plankConfig.getEnableMonitor() && AutomaticTrading.isTradeTime() && !monitoring.get() && TRACK_STOCK_MAP.size() > 0) {
            monitoring.set(true);
            executorService.submit(this::monitorStock);
            executorService.submit(this::queryMainFundData);
        }
    }

    /**
     * 复盘就会发现,大的亏损都是不遵守卖出原则导致的,对分时图的下跌趋势存在幻想,幻想它会扭转下跌趋势
     * 当然,不排除会在你卖出之后走强,但是首要原则是防止亏损,因为由赚到亏是非常伤害心态的
     * 有4条硬性止损原则必须执行,抛弃幻想
     * 从盈利到跌破成本 | 跌破止损位7% | 跌破MA10 | 当日亏损达到总仓位-2%    核
     * 止盈原则
     * 可以分批止盈 | 大幅脉冲无量可以清 | 脉冲之后跌破均线反弹无力可以清
     * 只做主升,不做震荡和下跌趋势
     * MA5均线之上的龙头可逢低参与
     * 2板梯队对比选强,竞价上底仓,上板加
     */
    private void monitorStock() {
        try {
            List<StockRealTimePrice> realTimePrices = new ArrayList<>();
            while (AutomaticTrading.isTradeTime()) {
                List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                        .in(Stock::getName, TRACK_STOCK_MAP.keySet()));
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
                log.error("------------------------ 主力净流入与板块 --------------------------");
                List<StockMainFundSample> topTen = new ArrayList<>();
                for (int i = 0; i < Math.min(MAIN_FUND_DATA.size(), 10); i++) {
                    topTen.add(MAIN_FUND_DATA.get(i));
                }
                log.warn(collectionToString(topTen.stream().map(e -> e.getF14() + e.getF3()).collect(Collectors.toList())));
                ArrayList<Bk> bks = Lists.newArrayList(TOP5_BK.values());
                Collections.sort(bks);
                log.warn(collectionToString(bks.stream().map(e -> e.getName() + e.getIncreaseRate() + "%").collect(Collectors.toList())));
                List<StockRealTimePrice> shareholding = realTimePrices.stream().filter(e -> TRACK_STOCK_MAP.containsKey(e.getName()) &&
                        TRACK_STOCK_MAP.get(e.getName()).getShareholding()).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(shareholding)) {
                    log.error("------------------------------- 持仓 ------------------------------");
                    shareholding.forEach(this::print);
                }
                realTimePrices.removeIf(e -> TRACK_STOCK_MAP.containsKey(e.getName()) && TRACK_STOCK_MAP.get(e.getName()).getShareholding());
                List<StockRealTimePrice> stockRealTimePrices = realTimePrices.stream().filter(e ->
                        e.getPurchaseRate() >= -2).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(stockRealTimePrices)) {
                    log.error("------------------------------ Track ------------------------------");
                    stockRealTimePrices.forEach(this::print);
                }
                List<HoldShares> buyStocks = holdSharesMapper.selectList(new LambdaQueryWrapper<HoldShares>()
                        .ge(HoldShares::getBuyTime, DateUtil.beginOfDay(new Date()))
                        .le(HoldShares::getBuyTime, DateUtil.endOfDay(new Date()))
                        .eq(HoldShares::getAutomaticTradingType, AutomaticTradingEnum.AUTO_PLANK.name()));
                if (CollectionUtils.isNotEmpty(buyStocks)) {
                    log.warn("排单金额:{} {}", AutomaticTrading.TODAY_COST_MONEY.intValue(),
                            collectionToString(buyStocks.stream().map(HoldShares::getName).collect(Collectors.toSet())));
                }
                if (plankConfig.getAutomaticPlankTrading() && automaticPlankTrading.openAutoPlank()) {
                    log.warn("一级缓存:{}", collectionToString(AutomaticPlankTrading.PLANK_LEVEL1_CACHE.values().stream()
                            .map(Stock::getName).collect(Collectors.toList())));
                    log.warn("打板监测:{}", collectionToString(AutomaticPlankTrading.PLANKING_CACHE.values().stream()
                            .map(Stock::getName).collect(Collectors.toList())));
                }
                if (CollectionUtils.isNotEmpty(AutomaticTrading.SALE_STOCK_CACHE)) {
                    log.error("止盈止损:{}", collectionToString(AutomaticTrading.SALE_STOCK_CACHE));
                }
                realTimePrices.clear();
                Thread.sleep(5000);
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
            this.updateStockPool();
            this.resetStockData();
            CountDownLatch countDownLatch = new CountDownLatch(Barbarossa.ALL_STOCK_MAP.size());
            dailyRecordProcessor.run(Barbarossa.ALL_STOCK_MAP, countDownLatch);
            countDownLatch.await();
            List<List<String>> partition = Lists.partition(Lists.newArrayList(Barbarossa.ALL_STOCK_MAP.keySet()), 300);
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
     * 更新股票池
     */
    private void updateStockPool() {
        BigDecimal zero = new BigDecimal(0);
        Date now = new Date();
        boolean haveNext = true;
        int page = 1;
        while (haveNext) {
            String url = plankConfig.getUpdateAllStockUrl().replace("{page}", page + "");
            String body = HttpUtil.getHttpGetResponseString(url, plankConfig.getXueQiuCookie());
            JSONObject data = JSON.parseObject(body).getJSONObject("data");
            Integer total = data.getInteger("count");
            JSONArray list = data.getJSONArray("list");
            for (Object o : list) {
                try {
                    JSONObject jsonObject = (JSONObject) o;
                    Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getCode, jsonObject.getString("symbol")));
                    if (Objects.isNull(stock)) {
                        stock = Stock.builder().code(jsonObject.getString("symbol")).name(jsonObject.getString("name")).marketValue(jsonObject.getLongValue("mc")).currentPrice(jsonObject.getBigDecimal("current")).purchasePrice(zero).transactionAmount(zero).purchaseType(10).track(false).shareholding(false).abbreviation("").automaticTradingType(AutomaticTradingEnum.CANCEL.name()).buyAmount(0).suckTriggerPrice(zero).build();
                        stockMapper.insert(stock);
                        stockInfoDao.insert(StockInfo.builder().code(stock.getCode().substring(2, 8)).name(stock.getName()).exchange(stock.getCode().substring(0, 2).toLowerCase()).state(0).type(0).createTime(now).updateTime(now).abbreviation("").build());
                        dailyIndexMapper.insert(DailyIndex.builder().code(stock.getCode().toLowerCase()).date(now).preClosingPrice(zero).openingPrice(zero).highestPrice(zero).lowestPrice(zero).tradingValue(zero)
                                .closingPrice(zero).tradingVolume(jsonObject.getLongValue("volume")).rurnoverRate(zero).createTime(now).updateTime(now).build());
                        log.info("股票池新增股票 {}", stock.getName());
                    } else {
                        stock.setName(jsonObject.getString("name"));
                        stockMapper.updateById(stock);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (page * 30 < total) {
                page++;
            } else {
                haveNext = false;
            }
        }
    }

    /**
     * 防止忘记当日复盘,重置stock表,持仓表数据,更新股票可用数量
     */
    private void resetStockData() {
        stockMapper.update(Stock.builder().plankNumber(0).automaticTradingType(AutomaticTradingEnum.CANCEL.name()).suckTriggerPrice(new BigDecimal(0)).buyAmount(0).build(), new LambdaUpdateWrapper<>());
        List<HoldShares> holdShares = holdSharesMapper.selectList(new LambdaQueryWrapper<HoldShares>()
                .ge(HoldShares::getBuyTime, DateUtil.beginOfDay(new Date()))
                .le(HoldShares::getBuyTime, DateUtil.endOfDay(new Date()))
                .gt(HoldShares::getNumber, 0));
        if (CollectionUtils.isNotEmpty(holdShares)) {
            for (HoldShares holdShare : holdShares) {
                holdShare.setAvailableVolume(holdShare.getAvailableVolume() + holdShare.getNumber());
                holdShare.setNumber(0);
                holdShare.setTodayPlank(false);
                holdSharesMapper.updateById(holdShare);
            }
        }
    }

    private String convertLog(StockRealTimePrice realTimePrice) {
        return realTimePrice.getName() + (realTimePrice.getName().length() == 3 ? "  " : "") +
                ">高:" + realTimePrice.getHighestPrice() + "|现:" + realTimePrice.getCurrentPrice() +
                "|低:" + realTimePrice.getLowestPrice() + "|" + realTimePrice.getIncreaseRate() + "%";
    }
}
