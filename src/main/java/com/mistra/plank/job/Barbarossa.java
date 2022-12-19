package com.mistra.plank.job;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.NamedThreadFactory;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.common.util.HttpUtil;
import com.mistra.plank.common.util.UploadDataListener;
import com.mistra.plank.dao.*;
import com.mistra.plank.model.dto.StockMainFundSample;
import com.mistra.plank.model.dto.StockRealTimePrice;
import com.mistra.plank.model.dto.UpwardTrendSample;
import com.mistra.plank.model.entity.DailyRecord;
import com.mistra.plank.model.entity.ForeignFundHoldingsTracking;
import com.mistra.plank.model.entity.Stock;
import com.mistra.plank.model.param.FundHoldingsParam;
import com.mistra.plank.service.Plank;
import com.mistra.plank.service.impl.ScreeningStocks;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
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

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
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
    private final FundHoldingsTrackingMapper fundHoldingsTrackingMapper;

    public static final ExecutorService executorService = new ThreadPoolExecutor(10, 20, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(5000), new NamedThreadFactory("T", false));
    /**
     * 主力趋势流入 过滤金额 >3亿
     */
    private final Integer mainFundFilterAmount = 300000000;
    public static final Integer W = 10000;
    /**
     * 所有股票 key-code value-name
     */
    public static final HashMap<String, String> STOCK_MAP = new HashMap<>();
    /**
     * 所有股票 name
     */
    public static final HashSet<String> STOCK_NAME_SET = new HashSet<>();
    /**
     * 需要监控关注的票 key-name value-Stock
     */
    public static final HashMap<String, Stock> TRACK_STOCK_MAP = new HashMap<>();

    public static final CopyOnWriteArrayList<StockMainFundSample> mainFundData = new CopyOnWriteArrayList<>();
    public static final CopyOnWriteArrayList<StockMainFundSample> mainFundDataAll = new CopyOnWriteArrayList<>();
    public static final ConcurrentHashMap<String, StockMainFundSample> mainFundDataMap = new ConcurrentHashMap<>(64);
    public static final ConcurrentHashMap<String, StockMainFundSample> mainFundDataAllMap =
            new ConcurrentHashMap<>(5000);

    /**
     * 总金额
     */
    public static BigDecimal BALANCE = new BigDecimal(100 * W);
    /**
     * 可用金额
     */
    public static BigDecimal BALANCE_AVAILABLE = new BigDecimal(100 * W);

    /**
     * 是否开启监控中
     */
    private final AtomicBoolean monitoring = new AtomicBoolean(false);

    public Barbarossa(StockMapper stockMapper, StockProcessor stockProcessor, DailyRecordMapper dailyRecordMapper,
                      ClearanceMapper clearanceMapper, TradeRecordMapper tradeRecordMapper, HoldSharesMapper holdSharesMapper,
                      Plank plank, PlankConfig plankConfig, ScreeningStocks screeningStocks,
                      DailyRecordProcessor dailyRecordProcessor, FundHoldingsTrackingMapper fundHoldingsTrackingMapper) {
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
        this.fundHoldingsTrackingMapper = fundHoldingsTrackingMapper;
    }

    @Override
    public void run(String... args) {
        List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>().notLike("name", "%ST%")
                .notLike("name", "%st%").notLike("name", "%A%").notLike("name", "%C%").notLike("name", "%N%")
                .notLike("name", "%U%").notLike("name", "%W%").notLike("code", "%BJ%").notLike("code", "%688%"));
        stocks.forEach(e -> {
            if (!e.getIgnoreMonitor() && (e.getShareholding() || e.getTrack())) {
                TRACK_STOCK_MAP.put(e.getName(), e);
            }
            STOCK_MAP.put(e.getCode(), e.getName());
            STOCK_NAME_SET.add(e.getName());
        });
    }

    @Scheduled(cron = "0 55 15 * * ?")
    private void analyzeData() {
        try {
            // 15点后读取当日交易数据
            dailyRecordProcessor.run(Barbarossa.STOCK_MAP);
            // 更新每只股票收盘价，当日成交量，MA5 MA10 MA20
            stockProcessor.run();
            // 更新 外资+基金 持仓 只更新到最新季度报告的汇总表上 基金季报有滞后性，外资持仓则是实时计算，每天更新的
            updateForeignFundShareholding(202203);
            // 分析连板数据
            analyzePlank();
            // 分析主力流入数据
            analyzeMainFund();
            // 分析日k均线多头排列的股票
            movingAverageRise();
            // 分析上升趋势的股票，周k均线多头排列
            analyzeUpwardTrend();
            // 爆量回踩
            screeningStocks.explosiveVolumeBack(new Date());
            // 分析红三兵股票
            screeningStocks.checkRedThreeSoldiersStock(new Date());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 找出日k均线多头排列的股票
     */
    private void movingAverageRise() {
        List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>().eq(Stock::getIgnoreMonitor, false)
                .ge(Stock::getMa5, 0).ge(Stock::getTransactionAmount, 500000000));
        List<String> list = stocks.stream().filter(stock -> stock.getMa5().compareTo(stock.getMa10()) > 0
                && stock.getMa10().compareTo(stock.getMa20()) > 0).map(stock -> StringUtils.substring(stock.getCode(), 2, 8))
                .collect(Collectors.toList());
        log.warn("日k均线多头排列:{}", collectionToString(list));
    }

    /**
     * 找出周均线向上发散，上升趋势的股票
     * 周均线MA03>MA05>MA10>MA20
     * 上市不足20个交易日的次新股就不计算了
     * 趋势股选出来之后我一般会直接用.txt文档导入到东方财富windows版客户端，再来人为筛选一遍k线好看的票
     */
    private void analyzeUpwardTrend() {
        List<UpwardTrendSample> samples = new ArrayList<>(STOCK_MAP.size());
        List<String> failed = new ArrayList<>();
        for (Map.Entry<String, String> entry : STOCK_MAP.entrySet()) {
            Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getCode, entry.getKey()));
            if (stock.getIgnoreMonitor() || stock.getTrack()
                    || stock.getTransactionAmount().doubleValue() < 500000000) {
                continue;
            }
            List<DailyRecord> dailyRecords = dailyRecordMapper
                    .selectList(new LambdaQueryWrapper<DailyRecord>().eq(DailyRecord::getCode, entry.getKey())
                            .ge(DailyRecord::getDate, DateUtils.addDays(new Date(), -200)).orderByDesc(DailyRecord::getDate));
            if (dailyRecords.size() < 100) {
                failed.add(entry.getKey());
                continue;
            }
            dailyRecords = dailyRecords.subList(0, 100);
            // 计算周k线，直接取5天为默认一周，不按自然周计算。先把100条日交易记录转换为20周k线。周五收盘价即为当前周收盘价。
            List<BigDecimal> week = new ArrayList<>();
            for (int i = 0; i < dailyRecords.size(); i += 5) {
                week.add(dailyRecords.get(i).getClosePrice());
            }
            // 计算MA3
            double ma3 = week.subList(0, 3).stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue));
            // 计算MA5
            double ma5 = week.subList(0, 5).stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue));
            // 计算MA10
            double ma10 = week.subList(0, 10).stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue));
            // 计算MA20
            double ma20 = week.stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue));
            if (ma3 > ma5 && ma5 > ma10 && ma10 > ma20) {
                // 计算方差
                double variance = variance(new double[]{ma3, ma5, ma10, ma20});
                samples.add(UpwardTrendSample.builder().ma3(new BigDecimal(ma3).setScale(2, RoundingMode.HALF_UP))
                        .ma5(new BigDecimal(ma5).setScale(2, RoundingMode.HALF_UP))
                        .ma10(new BigDecimal(ma10).setScale(2, RoundingMode.HALF_UP))
                        .ma20(new BigDecimal(ma20).setScale(2, RoundingMode.HALF_UP)).name(entry.getValue())
                        .code(StringUtils.substring(entry.getKey(), 2, 8)).variance(variance).build());
            }
        }
        if (CollectionUtils.isNotEmpty(failed)) {
//            log.error("{}的交易数据不完整(可能是次新股，上市不足100个交易日)", collectionToString(failed));
        }
        Collections.sort(samples);
        log.warn("新发现的上升趋势的股票一共[{}]支:{}", samples.size(),
                collectionToString(samples.stream()
                        .map(plankConfig.getPrintName() ? UpwardTrendSample::getName : UpwardTrendSample::getCode)
                        .collect(Collectors.toSet())));
        if (CollectionUtils.isNotEmpty(samples)) {
            // 找出来之后直接更新这些股票为监控股票
            List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>().in(Stock::getCode,
                    samples.stream().map(UpwardTrendSample::getCode).collect(Collectors.toSet())));
            for (Stock stock : stocks) {
                stock.setTrack(true);
                // stockMapper.updateById(stock);
            }
        }
    }

    /**
     * 求方差
     *
     * @param x 数组
     * @return 方差
     */
    public static double variance(double[] x) {
        int m = x.length;
        double sum = 0;
        for (double v : x) {
            sum += v;
        }
        double dAve = sum / m;
        double dVar = 0;
        for (double v : x) {
            dVar += (v - dAve) * (v - dAve);
        }
        return dVar / m;
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
            executorService.submit(this::monitorStock);
            executorService.submit(this::queryMainFundData);
            executorService.submit(this::smallMarketMainFundData);
            monitoring.set(true);
        }
    }

    /**
     * 50亿以下，实时净流入大5000万
     * 50-100亿的，实时净流入大于1亿
     */
    private void smallMarketMainFundData() {
        // 取市值10亿到100亿的股票
        List<Stock> smallMarketStocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>().le(Stock::getMarketValue, 10000000000L)
                .ge(Stock::getMarketValue, 1000000000L));
        if (CollectionUtils.isNotEmpty(smallMarketStocks)) {
            Set<String> set = smallMarketStocks.stream().map(Stock::getName).collect(Collectors.toSet());
            while (AutomaticTrading.isTradeTime()) {
                try {
                    String body = HttpUtil.getHttpGetResponseString(plankConfig.getMainFundUrl(), null);
                    JSONArray array = JSON.parseObject(body).getJSONObject("data").getJSONArray("diff");
                    List<StockMainFundSample> tmpList = new ArrayList<>();
                    List<String> stock = new ArrayList<>();
                    array.parallelStream().forEach(e -> {
                        try {
                            StockMainFundSample mainFundSample = JSONObject.parseObject(e.toString(), StockMainFundSample.class);
                            tmpList.add(mainFundSample);
                            if (set.contains(mainFundSample.getF14()) && mainFundSample.getF62() > 30000000) {
                                // 主力流入>3kw
                                stock.add(mainFundSample.getF14());
                            }
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    });
                    log.warn("市值[10e,100e]主力流入>3kw:{}", collectionToString(stock));
                    Thread.sleep(3000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void monitorStock() {
        try {
            List<StockRealTimePrice> realTimePrices = new ArrayList<>();
            while (AutomaticTrading.isTradeTime()) {
                List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>().eq(Stock::getIgnoreMonitor, false)
                        .eq(Stock::getTrack, true).or().eq(Stock::getShareholding, true));
                Map<String, Stock> stockMap = stocks.stream().collect(Collectors.toMap(Stock::getName, e -> e));
                for (Stock stock : stocks) {
                    // 默认把MA10作为建仓基准价格
                    int purchaseType = Objects.isNull(stock.getPurchaseType()) || stock.getPurchaseType() == 0 ? 10
                            : stock.getPurchaseType();
                    List<DailyRecord> dailyRecords =
                            dailyRecordMapper.selectList(new LambdaQueryWrapper<DailyRecord>().eq(DailyRecord::getCode, stock.getCode())
                                    .ge(DailyRecord::getDate, DateUtils.addDays(new Date(), -purchaseType * 3)).orderByDesc(DailyRecord::getDate));
                    if (dailyRecords.size() < purchaseType) {
                        log.error("{}的交易数据不完整，不够{}个交易日数据！请先爬取交易数据！", stock.getCode(), stock.getPurchaseType());
                        continue;
                    }
                    StockRealTimePrice stockRealTimePrice = stockProcessor.getStockRealTimePriceByCode(stock.getCode());
                    double v = stockRealTimePrice.getTodayRealTimePrice();
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
                            ? mainFundDataMap.get(stock.getName()).getF62() / W : 0);
                    stockRealTimePrice.setPurchasePrice(maPrice);
                    stockRealTimePrice.setPurchaseRate((int) (purchaseRate * 100));
                    realTimePrices.add(stockRealTimePrice);
                }
                Collections.sort(realTimePrices);
                System.out.println("\n\n\n");
                log.error("------------------------ ↓主力净流入Top10↓ -------------------------");
                List<StockMainFundSample> topTen = new ArrayList<>();
                for (int i = 0; i < Math.min(mainFundDataAll.size(), 10); i++) {
                    topTen.add(mainFundDataAll.get(i));
                }
                log.warn(collectionToString(
                        topTen.stream().map(e -> e.getF14() + "[" + e.getF62() / W / W + "亿]" + e.getF3() + "%")
                                .collect(Collectors.toList())));
                log.error("------------------------------ ↓持仓↓ -----------------------------");
                for (StockRealTimePrice realTimePrice : realTimePrices) {
                    if (stockMap.get(realTimePrice.getName()).getShareholding()) {
                        if (realTimePrice.getIncreaseRate() > 0) {
                            Barbarossa.log.error(convertLog(realTimePrice));
                        } else {
                            Barbarossa.log.warn(convertLog(realTimePrice));
                        }
                    }
                }
                realTimePrices.removeIf(e -> stockMap.get(e.getName()).getShareholding());
                log.error("------------------------------ ↓建仓↓ -----------------------------");
                for (StockRealTimePrice realTimePrice : realTimePrices) {
                    if (realTimePrice.getPurchaseRate() >= -2) {
                        Barbarossa.log.warn(convertLog(realTimePrice));
                    }
                }
//                log.error("---------------------------- ↓打板排单↓ ----------------------------");
//                List<Stock> buyStocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>().ge(Stock::getBuyTime, DateUtil.beginOfDay(new Date()))
//                        .le(Stock::getBuyTime, DateUtil.endOfDay(new Date())));
//                for (Stock buyStock : buyStocks) {
//                    log.warn("{} 数量:{},金额:{}", buyStock.getName(), buyStock.getBuyAmount(), buyStock.getBuyPrice());
//                }
//                log.error("---------------------------- ↓打板监测↓ ----------------------------");
//                if (CollectionUtils.isNotEmpty(AutomaticTrading.runningMap.values())) {
//                    log.warn("{}", collectionToString(AutomaticTrading.runningMap.values().stream().map(Stock::getName).collect(Collectors.toList())));
//                }
                realTimePrices.clear();
                Thread.sleep(5000);
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
                        if (TRACK_STOCK_MAP.containsKey(mainFundSample.getF14())) {
                            mainFundDataMap.put(mainFundSample.getF14(), mainFundSample);
                        }
                    } catch (JSONException ignored) {
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                });
                List<StockMainFundSample> result = tmpList.stream().filter(e -> e != null && STOCK_NAME_SET.contains(e.getF14()) && e.getF62() != null)
                        .sorted().collect(Collectors.toList());
                mainFundDataAll.clear();
                mainFundDataAll.addAll(result.stream().filter(e -> e.getF62() > 100000000).collect(Collectors.toList()));
                mainFundData.clear();
                mainFundData.addAll(
                        result.stream().filter(e -> TRACK_STOCK_MAP.containsKey(e.getF14())).collect(Collectors.toList()));
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
                .append("[高:").append(realTimePrice.getTodayHighestPrice())
                .append("|现:").append(realTimePrice.getTodayRealTimePrice())
                .append("|低:").append(realTimePrice.getTodayLowestPrice())
                .append("|差距:").append(realTimePrice.getPurchaseRate())
                .append("%|涨幅:").append(realTimePrice.getIncreaseRate())
                .append("|主力:").append(realTimePrice.getMainFund()).append("万]").toString();
    }

    private void analyzeMainFund() {
        log.warn("3|5|10日主力净流入>3亿:" + collectionToString(mainFundDataAll.parallelStream()
                .filter(e -> e.getF267() > mainFundFilterAmount || e.getF164() > mainFundFilterAmount
                        || e.getF174() > mainFundFilterAmount)
                .map(plankConfig.getPrintName() ? StockMainFundSample::getF14 : StockMainFundSample::getF12)
                .collect(Collectors.toSet())));
    }

    /**
     * 分析最近一个月各连板晋级率
     */
    public void analyzePlank() {
        // 5连板+的股票
        HashSet<String> fivePlankStock = new HashSet<>();
        HashMap<String, Integer> gemPlankStockNumber = new HashMap<>();
        Date date = new DateTime(DateUtils.addDays(new Date(), -30)).withHourOfDay(0).withMinuteOfHour(0)
                .withSecondOfMinute(0).withMillisOfSecond(0).toDate();
        // 首板一进二胜率
        HashMap<String, BigDecimal> oneToTwo = new HashMap<>(64);
        // 二板二进三胜率
        HashMap<String, BigDecimal> twoToThree = new HashMap<>(64);
        // 三板三进四胜率
        HashMap<String, BigDecimal> threeToFour = new HashMap<>(32);
        // 四板四进五胜率
        HashMap<String, BigDecimal> fourToFive = new HashMap<>(16);
        // 五板五进六胜率
        HashMap<String, BigDecimal> fiveToSix = new HashMap<>(16);
        // 六板六进七胜率
        HashMap<String, BigDecimal> sixToSeven = new HashMap<>(16);
        List<DailyRecord> dailyRecords =
                dailyRecordMapper.selectList(new LambdaQueryWrapper<DailyRecord>().ge(DailyRecord::getDate, date));
        Map<String, List<DailyRecord>> dateListMap =
                dailyRecords.stream().collect(Collectors.groupingBy(dailyRecord -> sdf.format(dailyRecord.getDate())));
        // 昨日首板
        HashMap<String, Double> yesterdayOne = new HashMap<>(64);
        // 昨日二板
        HashMap<String, Double> yesterdayTwo = new HashMap<>(32);
        // 昨日三板
        HashMap<String, Double> yesterdayThree = new HashMap<>(16);
        // 昨日四板
        HashMap<String, Double> yesterdayFour = new HashMap<>(8);
        // 昨日五板
        HashMap<String, Double> yesterdayFive = new HashMap<>(4);
        // 昨日六板
        HashMap<String, Double> yesterdaySix = new HashMap<>(4);
        do {
            List<DailyRecord> records = dateListMap.get(sdf.format(date));
            if (CollectionUtils.isNotEmpty(records)) {
                // 今日首板
                HashMap<String, Double> todayOne = new HashMap<>(64);
                // 今日二板
                HashMap<String, Double> todayTwo = new HashMap<>(32);
                // 今日三板
                HashMap<String, Double> todayThree = new HashMap<>(16);
                // 今日四板
                HashMap<String, Double> todayFour = new HashMap<>(16);
                // 今日五板
                HashMap<String, Double> todayFive = new HashMap<>(16);
                // 今日六板
                HashMap<String, Double> todaySix = new HashMap<>(16);
                // 今日七板
                HashMap<String, Double> todaySeven = new HashMap<>(16);
                for (DailyRecord dailyRecord : records) {
                    double v = dailyRecord.getIncreaseRate().doubleValue();
                    String name = dailyRecord.getName();
                    String code = dailyRecord.getCode();
                    if (code.contains("SZ30") && v > 19.4 && v < 21) {
                        gemPlankStockNumber.put(name, gemPlankStockNumber.getOrDefault(name, 0) + 1);
                    }
                    if ((!code.contains("SZ30") && v > 9.4 && v < 11)
                            || (code.contains("SZ30") && v > 19.4 && v < 21)) {
                        if (yesterdaySix.containsKey(name)) {
                            // 昨日的六板，今天继续板，进阶7板
                            todaySeven.put(dailyRecord.getName(), v);
                        } else if (yesterdayFive.containsKey(name)) {
                            // 昨日的五板，今天继续板，进阶6板
                            todaySix.put(dailyRecord.getName(), v);
                        } else if (yesterdayFour.containsKey(name)) {
                            // 昨日的四板，今天继续板，进阶5板
                            todayFive.put(dailyRecord.getName(), v);
                        } else if (yesterdayThree.containsKey(name)) {
                            // 昨日的三板，今天继续板，进阶4板
                            todayFour.put(dailyRecord.getName(), v);
                        } else if (yesterdayTwo.containsKey(name)) {
                            // 昨日的二板，今天继续板，进阶3板
                            todayThree.put(dailyRecord.getName(), v);
                        } else if (yesterdayOne.containsKey(name)) {
                            // 昨日首板，今天继续板，进阶2板
                            todayTwo.put(dailyRecord.getName(), v);
                        } else {
                            // 昨日没有板，今日首板
                            todayOne.put(dailyRecord.getName(), v);
                        }
                    }
                }
                this.promotion(oneToTwo, todayTwo, yesterdayOne, date);
                this.promotion(twoToThree, todayThree, yesterdayTwo, date);
                this.promotion(threeToFour, todayFour, yesterdayThree, date);
                this.promotion(fourToFive, todayFive, yesterdayFour, date);
                this.promotion(fiveToSix, todaySix, yesterdayFive, date);
                this.promotion(sixToSeven, todaySeven, yesterdaySix, date);
                if (date.after(DateUtils.addDays(new Date(), -5))) {
                    // 只打印最近3-5个交易日的连板数据
                    log.warn("{}日连板数据：" + "\n一板{}支:{}\n二板{}支:{}\n三板{}支:{}\n四板{}支:{}\n五板{}支:{}\n六板{}支:{}\n七板{}支:{}",
                            sdf.format(date), todayOne.keySet().size(), new ArrayList<>(todayOne.keySet()),
                            todayTwo.keySet().size(), new ArrayList<>(todayTwo.keySet()), todayThree.keySet().size(),
                            new ArrayList<>(todayThree.keySet()), todayFour.keySet().size(),
                            new ArrayList<>(todayFour.keySet()), todayFive.keySet().size(),
                            new ArrayList<>(todayFive.keySet()), todaySix.keySet().size(),
                            new ArrayList<>(todaySix.keySet()), todaySeven.keySet().size(),
                            new ArrayList<>(todaySeven.keySet()));
                    List<String> tmp = new ArrayList<>();
                    tmp.addAll(todayTwo.keySet());
                    tmp.addAll(todayThree.keySet());
                    tmp.addAll(todayFour.keySet());
                    tmp.addAll(todayFive.keySet());
                    tmp.addAll(todaySix.keySet());
                    tmp.addAll(todaySeven.keySet());
                    if (CollectionUtils.isNotEmpty(tmp)) {
                        List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>().in(Stock::getName, tmp));
                        tmp.clear();
                        for (Stock stock : stocks) {
                            tmp.add(stock.getCode().substring(2, 8));
                        }
                        log.warn("二板+:{}", collectionToString(tmp));
                    }
                }
                fivePlankStock.addAll(todayFive.keySet());
                yesterdayOne.clear();
                yesterdayOne.putAll(todayOne);
                yesterdayTwo.clear();
                yesterdayTwo.putAll(todayTwo);
                yesterdayThree.clear();
                yesterdayThree.putAll(todayThree);
                yesterdayFour.clear();
                yesterdayFour.putAll(todayFour);
                yesterdayFive.clear();
                yesterdayFive.putAll(todayFive);
                yesterdaySix.clear();
                yesterdaySix.putAll(todaySix);
            }
            date = DateUtils.addDays(date, 1);
        } while (date.getTime() < System.currentTimeMillis());
        List<String> gemPlankStockTwice = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : gemPlankStockNumber.entrySet()) {
            if (entry.getValue() > 1) {
                gemPlankStockTwice.add(entry.getKey());
            }
        }
        log.warn("最近一个月5连板+的股票:{}", collectionToString(fivePlankStock));
        log.warn("最近一个月创业板涨停2次+的股票:{}", collectionToString(gemPlankStockTwice));
        log.error("一板>一进二平均胜率：{}",
                (double) Math
                        .round(oneToTwo.values().stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100)
                        / 100);
        log.error("二板>二进三平均胜率：{}",
                (double) Math
                        .round(twoToThree.values().stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100)
                        / 100);
        log.error("三板>三进四平均胜率：{}",
                (double) Math
                        .round(threeToFour.values().stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100)
                        / 100);
        log.error("四板>四进五平均胜率：{}",
                (double) Math
                        .round(fourToFive.values().stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100)
                        / 100);
        log.error("五板>五进六平均胜率：{}",
                (double) Math
                        .round(fiveToSix.values().stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100)
                        / 100);
        log.error("六板>六进七平均胜率：{}",
                (double) Math
                        .round(sixToSeven.values().stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100)
                        / 100);
    }

    private void promotion(HashMap<String, BigDecimal> promotion, HashMap<String, Double> today,
                           HashMap<String, Double> yesterday, Date date) {
        if (yesterday.size() > 0) {
            promotion.put(sdf.format(date), divide(today.size(), yesterday.size()));
        }
    }

    private BigDecimal divide(double x, int y) {
        if (y <= 0) {
            return new BigDecimal(0);
        }
        return new BigDecimal(x).divide(new BigDecimal(y), 2, RoundingMode.HALF_UP);
    }

    /**
     * 以历史数据为样本，根据配置的买入，卖出，分仓策略自动交易
     */
    public void barbarossa(Integer fundsPart, Long beginDay) {
        // 清除老数据
        holdSharesMapper.delete(new QueryWrapper<>());
        clearanceMapper.delete(new QueryWrapper<>());
        tradeRecordMapper.delete(new QueryWrapper<>());
        BALANCE = new BigDecimal(100 * W);
        BALANCE_AVAILABLE = new BigDecimal(100 * W);
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
            if (CollectionUtils.isNotEmpty(stocks) && BALANCE_AVAILABLE.intValue() > W) {
                plank.buyStock(stocks, date, fundsPart);
            }
            plank.sellStock(date);
        }
    }

    public void fundHoldingsImport(FundHoldingsParam fundHoldingsParam, Date beginTime, Date endTime) {
        UploadDataListener<ForeignFundHoldingsTracking> uploadDataListener = new UploadDataListener<>(500);
        try {
            EasyExcel.read(fundHoldingsParam.getFile().getInputStream(), ForeignFundHoldingsTracking.class,
                    uploadDataListener).sheet().headRowNumber(2).doRead();
        } catch (IOException e) {
            log.error("read excel file error,file name:{}", fundHoldingsParam.getFile().getName());
        }
        for (Map.Entry<Integer, ForeignFundHoldingsTracking> entry : uploadDataListener.getMap().entrySet()) {
            executorService.submit(() -> {
                ForeignFundHoldingsTracking fundHoldingsTracking = entry.getValue();
                try {
                    Stock stock =
                            stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getName, fundHoldingsTracking.getName()));
                    fundHoldingsTracking.setCode(stock.getCode());
                    fundHoldingsTracking.setQuarter(fundHoldingsParam.getQuarter());
                    List<DailyRecord> dailyRecordList = dailyRecordMapper.selectList(new LambdaQueryWrapper<DailyRecord>()
                            .eq(DailyRecord::getName, fundHoldingsTracking.getName()).ge(DailyRecord::getDate, beginTime).le(DailyRecord::getDate, endTime));
                    if (CollectionUtils.isEmpty(dailyRecordList)) {
                        HashMap<String, String> stockMap = new HashMap<>();
                        stockMap.put(stock.getCode(), stock.getName());
                        dailyRecordProcessor.run(stockMap);
                        Thread.sleep(60 * 1000);
                        dailyRecordList = dailyRecordMapper.selectList(new LambdaQueryWrapper<DailyRecord>()
                                .eq(DailyRecord::getName, fundHoldingsTracking.getName()).ge(DailyRecord::getDate, beginTime).le(DailyRecord::getDate, endTime));
                    }
                    double average = dailyRecordList.stream().map(DailyRecord::getClosePrice)
                            .mapToInt(BigDecimal::intValue).average().orElse(0D);
                    fundHoldingsTracking.setAveragePrice(new BigDecimal(average));
                    fundHoldingsTracking
                            .setShareholdingChangeAmount(average * fundHoldingsTracking.getShareholdingChangeCount());
                    fundHoldingsTracking.setModifyTime(new Date());
                    fundHoldingsTracking.setForeignTotalMarketDynamic(0L);
                    fundHoldingsTracking.setForeignFundTotalMarketDynamic(0L);
                    fundHoldingsTrackingMapper.insert(fundHoldingsTracking);
                    log.warn("更新 [{}] {}季报基金持仓数据完成！", stock.getName(), fundHoldingsParam.getQuarter());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * 更新 外资+基金 持仓
     * 基金的实时持仓市值是根据该季度(quarter)季报公布的持仓股数*当日收盘价 计算的。所以跟实际情况肯定存在差距的，仅作为参考
     * 外资持仓市值是前一个交易日最新的数据，是实时的
     */
    private void updateForeignFundShareholding(Integer quarter) {
        HashMap<String, JSONObject> foreignShareholding = getForeignShareholding();
        List<ForeignFundHoldingsTracking> fundHoldings = fundHoldingsTrackingMapper
                .selectList(new LambdaQueryWrapper<ForeignFundHoldingsTracking>().eq(ForeignFundHoldingsTracking::getQuarter, quarter));
        List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                .in(Stock::getName, fundHoldings.stream().map(ForeignFundHoldingsTracking::getName).collect(Collectors.toList())));
        if (CollectionUtils.isEmpty(foreignShareholding.values()) || CollectionUtils.isEmpty(fundHoldings)
                || CollectionUtils.isEmpty(stocks)) {
            return;
        }
        Map<String, Stock> stockMap = stocks.stream().collect(Collectors.toMap(Stock::getName, e -> e));
        for (ForeignFundHoldingsTracking tracking : fundHoldings) {
            JSONObject jsonObject = foreignShareholding.get(tracking.getName());
            try {
                if (Objects.nonNull(jsonObject)) {
                    long foreignTotalMarket = jsonObject.getLong("HOLD_MARKET_CAP");
                    tracking.setForeignTotalMarketDynamic(foreignTotalMarket);
                }
                tracking.setFundTotalMarketDynamic(stockMap.get(tracking.getName()).getCurrentPrice()
                        .multiply(new BigDecimal(tracking.getShareholdingCount())).longValue());
                tracking.setForeignFundTotalMarketDynamic(
                        tracking.getFundTotalMarketDynamic() + tracking.getForeignTotalMarketDynamic());
                tracking.setModifyTime(new Date());
                fundHoldingsTrackingMapper.updateById(tracking);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.warn("股票最新外资持仓市值更新完成！");
    }

    /**
     * 获取外资持股明细 截止昨日的
     *
     * @return HashMap<String, JSONObject>
     */
    private HashMap<String, JSONObject> getForeignShareholding() {
        HashMap<String, JSONObject> result = new HashMap<>();
        try {
            int pageNumber = 1;
            while (pageNumber <= 30) {
                String body = HttpUtil.getHttpGetResponseString(
                        plankConfig.getForeignShareholdingUrl().replace("{pageNumber}", pageNumber + ""), null);
                body = body.substring(body.indexOf("(") + 1, body.indexOf(")"));
                JSONObject parseObject = JSON.parseObject(body);
                if (parseObject.getJSONObject("result") != null) {
                    JSONArray array = parseObject.getJSONObject("result").getJSONArray("data");
                    for (Object o : array) {
                        JSONObject jsonObject = (JSONObject) o;
                        result.put(jsonObject.getString("SECURITY_NAME"), jsonObject);
                    }
                }
                pageNumber++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
