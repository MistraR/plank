package com.mistra.plank.job;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.NamedThreadFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.mistra.plank.service.impl.ScreeningStocks;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import static com.mistra.plank.config.SystemConstant.W;

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
    private final AutomaticPlankTrading automaticPlankTrading;
    public static final ThreadPoolExecutor executorService = new ThreadPoolExecutor(availableProcessors * 2,
            availableProcessors * 2, 100L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(5000), new NamedThreadFactory("Plank-", false));
    /**
     * 所有股票 key-code value-name
     */
    public static final HashMap<String, String> STOCK_ALL_MAP = new HashMap<>(4096);
    /**
     * 需要监控关注的机构趋势票 key-name value-Stock
     */
    public static final HashMap<String, Stock> STOCK_TRACK_MAP = new HashMap<>(32);
    /**
     * 自动打板股票，一级过滤map
     */
    public static final HashMap<String, Stock> STOCK_AUTO_PLANK_FILTER_MAP = new HashMap<>(512);
    public static final CopyOnWriteArrayList<StockMainFundSample> mainFundDataAll = new CopyOnWriteArrayList<>();
    public static final ConcurrentHashMap<String, StockMainFundSample> mainFundDataAllMap =
            new ConcurrentHashMap<>(4096);
    /**
     * 是否开启监控中
     */
    private final AtomicBoolean monitoring = new AtomicBoolean(false);

    public Barbarossa(StockMapper stockMapper, StockProcessor stockProcessor, DailyRecordMapper dailyRecordMapper,
                      HoldSharesMapper holdSharesMapper, PlankConfig plankConfig, ScreeningStocks screeningStocks,
                      DailyRecordProcessor dailyRecordProcessor, AnalyzeProcessor analyzePlank,
                      AutomaticPlankTrading automaticPlankTrading) {
        this.stockMapper = stockMapper;
        this.stockProcessor = stockProcessor;
        this.dailyRecordMapper = dailyRecordMapper;
        this.holdSharesMapper = holdSharesMapper;
        this.plankConfig = plankConfig;
        this.screeningStocks = screeningStocks;
        this.dailyRecordProcessor = dailyRecordProcessor;
        this.analyzePlank = analyzePlank;
        this.automaticPlankTrading = automaticPlankTrading;
    }

    @Override
    public void run(String... args) {
        updateStockCache();
    }

    //@Scheduled(cron = "*/2 * * * * ?")
    private void executorStatus() {
        log.error("ThreadPoolExecutor core:{},max:{},queue:{}", Barbarossa.executorService.getCorePoolSize(),
                Barbarossa.executorService.getMaximumPoolSize(), Barbarossa.executorService.getQueue().size());
    }

    /**
     * 集合竞价结束，初始化股票基本数据
     */
    @Scheduled(cron = "0 25 9 * * ?")
    private void updateStockCache() {
        List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>()
                // 默认过滤掉了北交所,科创板,ST
                .notLike("name", "%ST%").notLike("code", "%688%")
                .notLike("name", "%st%").notLike("name", "%A%").notLike("name", "%N%")
                .notLike("name", "%U%").notLike("name", "%W%").notLike("code", "%BJ%"));
        STOCK_AUTO_PLANK_FILTER_MAP.clear();
        STOCK_TRACK_MAP.clear();
        stocks.forEach(e -> {
            if ((e.getShareholding() || e.getTrack())) {
                STOCK_TRACK_MAP.put(e.getName(), e);
            } else if (e.getTransactionAmount().doubleValue() > plankConfig.getStockTurnoverFilter()
                    && (Objects.isNull(e.getBuyTime()) || !DateUtils.isSameDay(new Date(), e.getBuyTime()))) {
                // 过滤掉成交额小于plankConfig.getStockTurnoverFilter()的股票,
                if (plankConfig.getAutomaticPlankTop5Bk() && CollectionUtils.isNotEmpty(StockProcessor.TOP5_BK.values())) {
                    String bk = StockProcessor.TOP5_BK.keySet().stream().filter(v -> Objects.nonNull(e.getClassification()) &&
                            e.getClassification().contains(v)).findFirst().orElse(null);
                    if (StringUtils.isNotEmpty(bk)) {
                        STOCK_AUTO_PLANK_FILTER_MAP.put(e.getCode(), e);
                    }
                } else {
                    STOCK_AUTO_PLANK_FILTER_MAP.put(e.getCode(), e);
                }
            }
            STOCK_ALL_MAP.put(e.getCode(), e.getName());
        });
        log.warn("实时加载[{}]支股票,添加到自动打板一级缓存[{}]支,是否只打涨幅Top5板块的成分股:{}",
                stocks.size(), STOCK_AUTO_PLANK_FILTER_MAP.size(), plankConfig.getAutomaticPlankTop5Bk());
    }

    /**
     * 开盘,初始化版块基本数据
     */
    @Scheduled(cron = "0 30 9 * * ?")
    private void opening() {
        // 更新行业版块，概念版块涨幅信息
        this.updateBkRealTimeData();
    }

    /**
     * 每3秒更新一次版块涨跌幅
     */
    @Scheduled(cron = "*/3 * * * * ?")
    private void updateBkCache() {
        if (AutomaticTrading.isTradeTime()) {
            // 更新行业版块，概念版块涨幅信息
            this.updateBkRealTimeData();
        }
    }

    /**
     * 更新版块实时数据
     */
    private void updateBkRealTimeData() {
        stockProcessor.updateBk();
        stockProcessor.updateTop5IncreaseRateBk();
    }

    /**
     * 每2分钟更新每支股票的成交额,开盘6分钟内不更新,开盘快速封板的票当日成交额可能比较少
     * 成交额满足阈值的会放入 STOCK_FILTER_MAP 去检测涨幅
     */
    @Scheduled(cron = "0 */2 * * * ?")
    private void updateStockRealTimeData() throws InterruptedException {
        Date openingTime = new Date();
        DateUtils.setHours(openingTime, 9);
        DateUtils.setMinutes(openingTime, 30);
        if (AutomaticTrading.isTradeTime() && DateUtils.addMinutes(new Date(), -6).getTime() > openingTime.getTime()) {
            StockProcessor.RESET_PLANK_NUMBER.set(false);
            List<List<String>> partition = Lists.partition(Lists.newArrayList(Barbarossa.STOCK_ALL_MAP.keySet()), 300);
            CountDownLatch countDownLatch = new CountDownLatch(partition.size());
            for (List<String> list : partition) {
                executorService.submit(() -> stockProcessor.run(list, countDownLatch));
            }
            countDownLatch.await();
            this.updateStockCache();
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
        if (plankConfig.getEnableMonitor() && AutomaticTrading.isTradeTime() &&
                !monitoring.get() && STOCK_TRACK_MAP.size() > 0) {
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
                    stockRealTimePrice.setMainFund(mainFundDataAllMap.containsKey(stock.getName())
                            ? mainFundDataAllMap.get(stock.getName()).getF62() / W : 0);
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
                log.warn(collectionToString(topTen.stream().map(e -> e.getF14() + "[" + e.getF62() /
                        W / W + "亿]" + e.getF3() + "%").collect(Collectors.toList())));
                log.error("------------------------- 版块涨幅Top5 --------------------------");
                ArrayList<Bk> bks = Lists.newArrayList(StockProcessor.TOP5_BK.values());
                Collections.sort(bks);
                log.warn(collectionToString(bks.stream().map(e -> e.getName() + ":" + e.getIncreaseRate()).collect(Collectors.toList())));
                List<StockRealTimePrice> shareholding = realTimePrices.stream().filter(e ->
                        STOCK_TRACK_MAP.get(e.getName()).getShareholding()).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(shareholding)) {
                    log.error("------------------------------ 持仓 -----------------------------");
                    shareholding.forEach(e -> {
                        if (e.getIncreaseRate() > 0) {
                            Barbarossa.log.error(convertLog(e));
                        } else {
                            Barbarossa.log.warn(convertLog(e));
                        }
                    });
                }
                realTimePrices.removeIf(e -> STOCK_TRACK_MAP.get(e.getName()).getShareholding());
                List<StockRealTimePrice> stockRealTimePrices = realTimePrices.stream().filter(e ->
                        e.getPurchaseRate() >= -2).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(stockRealTimePrices)) {
                    log.error("------------------------------ Track -----------------------------");
                    stockRealTimePrices.forEach(e -> Barbarossa.log.warn(convertLog(e)));
                }
                List<HoldShares> buyStocks = holdSharesMapper.selectList(new LambdaQueryWrapper<HoldShares>()
                        .ge(HoldShares::getBuyTime, DateUtil.beginOfDay(new Date()))
                        .le(HoldShares::getBuyTime, DateUtil.endOfDay(new Date())));
                if (CollectionUtils.isNotEmpty(buyStocks)) {
                    log.error("----------------------------- 打板排单 ----------------------------");
                    log.warn("{}", buyStocks.stream().map(HoldShares::getName).collect(Collectors.toSet()));
                    log.warn("排单金额:{}", AutomaticTrading.TODAY_COST_MONEY.intValue());
                    if (plankConfig.getAutomaticPlankTrading() && automaticPlankTrading.openAutoPlank()) {
                        log.warn("打板监测:{}", AutomaticPlankTrading.PLANK_MONITOR.values().stream()
                                .map(Stock::getName).collect(Collectors.toList()));
                    }
                }
                if (CollectionUtils.isNotEmpty(AutomaticTrading.UNDER_MONITORING.values())) {
                    log.error("--------------------------- 自定义交易监测 --------------------------");
                    log.warn("{}", collectionToString(AutomaticTrading.UNDER_MONITORING.values().stream()
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
                ArrayList<StockMainFundSample> tmpList = new ArrayList<>();
                array.forEach(e -> {
                    try {
                        StockMainFundSample mainFundSample = JSONObject.parseObject(e.toString(), StockMainFundSample.class);
                        tmpList.add(mainFundSample);
                        mainFundDataAllMap.put(mainFundSample.getF14(), mainFundSample);
                    } catch (JSONException ignored) {
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                });
                mainFundDataAll.clear();
                mainFundDataAll.addAll(tmpList.stream().filter(e -> e.getF62() > 100000000).collect(Collectors.toList()));
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
            countDownLatch.await();
            log.warn("每日涨跌明细、成交额、MA5、MA10、MA20更新完成");
            StockProcessor.RESET_PLANK_NUMBER.set(true);
            executorService.submit(stockProcessor::updateStockBkInfo);
            // 更新 外资+基金 持仓 只更新到最新季度报告的汇总表上 基金季报有滞后性，外资持仓则是实时计算，每天更新的
            executorService.submit(stockProcessor::updateForeignFundShareholding);
            executorService.submit(() -> {
                // 分析连板数据
                analyzePlank.analyzePlank();
                // 分析主力流入数据
                analyzePlank.analyzeMainFund();
                // 分析日k均线多头排列的股票
                //screeningStocks.movingAverageRise();
                // 分析上升趋势的股票，周k均线多头排列
                screeningStocks.upwardTrend();
                // 分析爆量回踩
                screeningStocks.explosiveVolumeBack();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
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

}
