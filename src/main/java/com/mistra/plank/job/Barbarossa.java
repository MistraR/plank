package com.mistra.plank.job;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mistra.plank.config.PlankConfig;
import com.mistra.plank.mapper.ClearanceMapper;
import com.mistra.plank.mapper.DailyRecordMapper;
import com.mistra.plank.mapper.FundHoldingsTrackingMapper;
import com.mistra.plank.mapper.HoldSharesMapper;
import com.mistra.plank.mapper.StockMapper;
import com.mistra.plank.mapper.TradeRecordMapper;
import com.mistra.plank.pojo.dto.StockMainFundSample;
import com.mistra.plank.pojo.dto.StockRealTimePrice;
import com.mistra.plank.pojo.dto.UpwardTrendSample;
import com.mistra.plank.pojo.entity.DailyRecord;
import com.mistra.plank.pojo.entity.ForeignFundHoldingsTracking;
import com.mistra.plank.pojo.entity.Stock;
import com.mistra.plank.pojo.param.FundHoldingsParam;
import com.mistra.plank.service.Plank;
import com.mistra.plank.service.impl.ScreeningStocks;
import com.mistra.plank.util.HttpUtil;
import com.mistra.plank.util.StringUtil;
import com.mistra.plank.util.UploadDataListener;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * ????????????
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

    private final ExecutorService executorService = new ThreadPoolExecutor(10, 20, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(5000), new NamedThreadFactory("T", false));
    /**
     * ?????????????????? ???????????? >3???
     */
    private final Integer mainFundFilterAmount = 300000000;
    public static final Integer W = 10000;
    /**
     * ???????????? key-code value-name
     */
    public static final HashMap<String, String> STOCK_MAP = new HashMap<>();
    /**
     * ???????????????????????? key-name value-Stock
     */
    public static final HashMap<String, Stock> TRACK_STOCK_MAP = new HashMap<>();

    public static final CopyOnWriteArrayList<StockMainFundSample> mainFundData = new CopyOnWriteArrayList<>();
    public static final CopyOnWriteArrayList<StockMainFundSample> mainFundDataAll = new CopyOnWriteArrayList<>();
    public static final ConcurrentHashMap<String, StockMainFundSample> mainFundDataMap = new ConcurrentHashMap<>(64);
    public static final ConcurrentHashMap<String, StockMainFundSample> mainFundDataAllMap =
            new ConcurrentHashMap<>(5000);

    /**
     * ?????????
     */
    public static BigDecimal BALANCE = new BigDecimal(100 * W);
    /**
     * ????????????
     */
    public static BigDecimal BALANCE_AVAILABLE = new BigDecimal(100 * W);

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
        });
        // ?????????????????????????????????????????????
        // dailyRecordProcessor.run("SZ300015", "????????????");
        if (DateUtil.hour(new Date(), true) >= 15) {
            executorService.submit(this::queryMainFundData);
            // 15??????????????????????????????
            dailyRecordProcessor.run(Barbarossa.STOCK_MAP);
            // ????????????????????????????????????????????????MA5 MA10 MA20
            stockProcessor.run();
            // ?????? ??????+?????? ?????? ????????????????????????????????????????????? ???????????????????????????????????????????????????????????????????????????
//            updateForeignFundShareholding(202201);
            // ??????????????????
            analyzePlank();
            // ????????????????????????
            analyzeMainFund();
            // ???????????????????????????
            analyzeUpwardTrend();
            // ????????????
            screeningStocks.explosiveVolumeBack(new Date());
            // ?????????????????????
            screeningStocks.checkRedThreeSoldiersStock(new Date());
        } else {
            // 15???????????????????????????
            monitor();
        }
    }

    /**
     * ???????????????????????????????????????????????????
     * <p>
     * ?????????MA03>MA05>MA10>MA20
     * <p>
     * ????????????20???????????????????????????????????????
     * <p>
     * ?????????????????????????????????????????????.txt???????????????????????????windows???????????????????????????????????????k???????????????
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
            // ?????????k???????????????5???????????????????????????????????????????????????100???????????????????????????20???k????????????????????????????????????????????????
            List<BigDecimal> week = new ArrayList<>();
            for (int i = 0; i < dailyRecords.size(); i += 5) {
                week.add(dailyRecords.get(i).getClosePrice());
            }
            // ??????MA3
            double ma3 = week.subList(0, 3).stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue));
            // ??????MA5
            double ma5 = week.subList(0, 5).stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue));
            // ??????MA10
            double ma10 = week.subList(0, 10).stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue));
            // ??????MA20
            double ma20 = week.stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue));
            if (ma3 > ma5 && ma5 > ma10 && ma10 > ma20) {
                // ????????????
                double variance = variance(new double[]{ma3, ma5, ma10, ma20});
                samples.add(UpwardTrendSample.builder().ma3(new BigDecimal(ma3).setScale(2, RoundingMode.HALF_UP))
                        .ma5(new BigDecimal(ma5).setScale(2, RoundingMode.HALF_UP))
                        .ma10(new BigDecimal(ma10).setScale(2, RoundingMode.HALF_UP))
                        .ma20(new BigDecimal(ma20).setScale(2, RoundingMode.HALF_UP)).name(entry.getValue())
                        .code(StringUtils.substring(entry.getKey(), 2, 8)).variance(variance).build());
            }
        }
        if (CollectionUtils.isNotEmpty(failed)) {
            // log.error("{}????????????????????????(?????????????????????????????????100????????????)", collectionToString(failed));
        }
        Collections.sort(samples);
        log.warn("???????????????????????????????????????[{}]???:{}", samples.size(),
                StringUtil.collectionToString(samples.stream()
                        .map(plankConfig.getPrintName() ? UpwardTrendSample::getName : UpwardTrendSample::getCode)
                        .collect(Collectors.toSet())));
        if (CollectionUtils.isNotEmpty(samples)) {
            // ??????????????????????????????????????????????????????
            List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>().in(Stock::getCode,
                    samples.stream().map(UpwardTrendSample::getCode).collect(Collectors.toSet())));
            for (Stock stock : stocks) {
                stock.setTrack(true);
                // stockMapper.updateById(stock);
            }
        }
    }

    /**
     * ?????????
     *
     * @param x ??????
     * @return ??????
     */
    public static double variance(double[] x) {
        int m = x.length;
        double sum = 0;
        for (int i = 0; i < m; i++) {
            sum += x[i];
        }
        double dAve = sum / m;
        double dVar = 0;
        for (int i = 0; i < m; i++) {
            dVar += (x[i] - dAve) * (x[i] - dAve);
        }
        return dVar / m;
    }

    /**
     * ???????????????????????????????????????????????????
     * <p>
     * ?????????????????? ?????????????????????????????????????????????????????????????????????
     * <p>
     * ????????????????????????????????????????????????stock?????????track?????????true
     * <p>
     * ?????????????????????????????????????????????????????????MA10?????????????????????????????????????????????stock.purchase_type??????????????????5-??????MA5???????????????,??????MA20
     * <p>
     * ????????????????????????????????????????????????????????????????????????
     */
    public void monitor() {
        executorService.submit(this::monitorStock);
        executorService.submit(this::queryMainFundData);
    }

    private void monitorStock() {
        try {
            List<StockRealTimePrice> realTimePrices = new ArrayList<>();
            while (true) {
                List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>().eq("ignore_monitor", false)
                        .eq("track", true).or().eq("shareholding", true));
                Map<String, Stock> stockMap = stocks.stream().collect(Collectors.toMap(Stock::getName, e -> e));
                for (Stock stock : stocks) {
                    // ?????????MA10????????????????????????
                    int purchaseType = Objects.isNull(stock.getPurchaseType()) || stock.getPurchaseType() == 0 ? 10
                            : stock.getPurchaseType();
                    List<DailyRecord> dailyRecords =
                            dailyRecordMapper.selectList(new QueryWrapper<DailyRecord>().eq("code", stock.getCode())
                                    .ge("date", DateUtils.addDays(new Date(), -purchaseType * 2)).orderByDesc("date"));
                    if (dailyRecords.size() < purchaseType) {
                        log.error("{}?????????????????????????????????{}????????????????????????????????????????????????", stock.getCode(), stock.getPurchaseType());
                        continue;
                    }
                    String url = plankConfig.getXueQiuStockDetailUrl().replace("{code}", stock.getCode())
                            .replace("{time}", String.valueOf(System.currentTimeMillis()))
                            .replace("{recentDayNumber}", "1");
                    String body = HttpUtil.getHttpGetResponseString(url, plankConfig.getXueQiuCookie());
                    JSONObject data = JSON.parseObject(body).getJSONObject("data");
                    JSONArray list = data.getJSONArray("item");
                    if (CollectionUtils.isNotEmpty(list)) {
                        for (Object o : list) {
                            double v = ((JSONArray) o).getDoubleValue(5);
                            List<BigDecimal> collect = dailyRecords.subList(0, purchaseType - 1).stream()
                                    .map(DailyRecord::getClosePrice).collect(Collectors.toList());
                            collect.add(new BigDecimal(v).setScale(2, RoundingMode.HALF_UP));
                            double ma = collect.stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue));
                            // ?????????????????????purchasePrice?????????stock.purchasePrice ??????????????? 2????????????????????????????????????
                            if (Objects.nonNull(stock.getPurchasePrice())
                                    && stock.getPurchasePrice().doubleValue() > 0) {
                                ma = Math.min(stock.getPurchasePrice().doubleValue(), ma);
                            }
                            BigDecimal maPrice = new BigDecimal(ma).setScale(2, RoundingMode.HALF_UP);
                            double purchaseRate = (double) Math.round(((maPrice.doubleValue() - v) / v) * 100) / 100;
                            realTimePrices.add(StockRealTimePrice.builder().todayRealTimePrice(v).name(stock.getName())
                                    .todayHighestPrice(((JSONArray) o).getDoubleValue(3))
                                    .todayLowestPrice(((JSONArray) o).getDoubleValue(4))
                                    .mainFund(mainFundDataMap.containsKey(stock.getName())
                                            ? mainFundDataMap.get(stock.getName()).getF62() / W : 0)
                                    .purchasePrice(maPrice).increaseRate(((JSONArray) o).getDoubleValue(7))
                                    .purchaseRate((int) (purchaseRate * 100)).build());
                        }
                    }
                }
                Collections.sort(realTimePrices);
                System.out.println("\n\n\n");
                log.error("???????????????Top10???");
                List<StockMainFundSample> topTen = new ArrayList<>();
                for (int i = 0; i < Math.min(mainFundDataAll.size(), 10); i++) {
                    topTen.add(mainFundDataAll.get(i));
                }
                log.warn(StringUtil.collectionToString(
                        topTen.stream().map(e -> e.getF14() + "[" + e.getF62() / W / W + "???]" + e.getF3() + "%")
                                .collect(Collectors.toList())));
                log.error("?????????");
                for (StockRealTimePrice realTimePrice : realTimePrices) {
                    if (stockMap.get(realTimePrice.getName()).getShareholding()) {
                        Barbarossa.log.warn(convertLog(realTimePrice));
                    }
                }
                realTimePrices.removeIf(e -> stockMap.get(e.getName()).getShareholding());
                log.error("?????????");
                for (StockRealTimePrice realTimePrice : realTimePrices) {
                    if (realTimePrice.getPurchaseRate() >= -1) {
                        Barbarossa.log.warn(convertLog(realTimePrice));
                    }
                }
                log.error("?????????");
                for (StockRealTimePrice realTimePrice : realTimePrices) {
                    if (realTimePrice.getIncreaseRate() < -5 && realTimePrice.getPurchaseRate() < -1) {
                        Barbarossa.log.warn(convertLog(realTimePrice));
                    }
                }
                realTimePrices.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????????????????????????????
     */
    private void queryMainFundData() {
        while (true) {
            try {
                String body = HttpUtil.getHttpGetResponseString(plankConfig.getMainFundUrl(), null);
                JSONArray array = JSON.parseObject(body).getJSONObject("data").getJSONArray("diff");
                List<StockMainFundSample> result = new ArrayList<>();
                array.parallelStream().forEach(e -> {
                    try {
                        StockMainFundSample mainFundSample =
                                JSONObject.parseObject(e.toString(), StockMainFundSample.class);
                        result.add(mainFundSample);
                        mainFundDataAllMap.put(mainFundSample.getF14(), mainFundSample);
                        if (TRACK_STOCK_MAP.containsKey(mainFundSample.getF14())) {
                            mainFundDataMap.put(mainFundSample.getF14(), mainFundSample);
                        }
                    } catch (Exception exception) {
                    }
                });
                Collections.sort(result);
                mainFundDataAll.clear();
                mainFundDataAll
                        .addAll(result.stream().filter(e -> e.getF62() > 100000000).collect(Collectors.toList()));
                mainFundData.clear();
                mainFundData.addAll(
                        result.stream().filter(e -> TRACK_STOCK_MAP.containsKey(e.getF14())).collect(Collectors.toList()));
                Thread.sleep(3000);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            } catch (Exception e) {

            }
        }
    }

    private String convertLog(StockRealTimePrice realTimePrice) {
        StringBuilder builder = new StringBuilder().append(realTimePrice.getName())
                .append((realTimePrice.getName().length() == 3 ? "  " : "")).append("[???:")
                .append(realTimePrice.getTodayHighestPrice()).append("|???:").append(realTimePrice.getTodayLowestPrice())
                .append("|???:").append(realTimePrice.getTodayRealTimePrice())
                // .append("|???:").append(realTimePrice.getPurchasePrice())
                .append("|??????:").append(realTimePrice.getPurchaseRate()).append("%|??????:")
                .append(realTimePrice.getIncreaseRate()).append("|??????:").append(realTimePrice.getMainFund()).append("???]");
        return builder.toString();
    }

    private void analyzeMainFund() {
        log.warn("3|5|10??????????????????>3???:" + StringUtil.collectionToString(mainFundDataAll.parallelStream()
                .filter(e -> e.getF267() > mainFundFilterAmount || e.getF164() > mainFundFilterAmount
                        || e.getF174() > mainFundFilterAmount)
                .map(plankConfig.getPrintName() ? StockMainFundSample::getF14 : StockMainFundSample::getF12)
                .collect(Collectors.toSet())));
    }

    /**
     * ???????????????????????????????????????
     */
    public void analyzePlank() {
        // 5??????+?????????
        HashSet<String> fivePlankStock = new HashSet<>();
        HashMap<String, Integer> gemPlankStockNumber = new HashMap<>();
        Date date = new DateTime(DateUtils.addDays(new Date(), -30)).withHourOfDay(0).withMinuteOfHour(0)
                .withSecondOfMinute(0).withMillisOfSecond(0).toDate();
        // ?????????????????????
        HashMap<String, BigDecimal> oneToTwo = new HashMap<>(64);
        // ?????????????????????
        HashMap<String, BigDecimal> twoToThree = new HashMap<>(64);
        // ?????????????????????
        HashMap<String, BigDecimal> threeToFour = new HashMap<>(32);
        // ?????????????????????
        HashMap<String, BigDecimal> fourToFive = new HashMap<>(16);
        // ?????????????????????
        HashMap<String, BigDecimal> fiveToSix = new HashMap<>(16);
        // ?????????????????????
        HashMap<String, BigDecimal> sixToSeven = new HashMap<>(16);
        List<DailyRecord> dailyRecords =
                dailyRecordMapper.selectList(new LambdaQueryWrapper<DailyRecord>().ge(DailyRecord::getDate, date));
        Map<String, List<DailyRecord>> dateListMap =
                dailyRecords.stream().collect(Collectors.groupingBy(dailyRecord -> sdf.format(dailyRecord.getDate())));
        // ????????????
        HashMap<String, Double> yesterdayOne = new HashMap<>(64);
        // ????????????
        HashMap<String, Double> yesterdayTwo = new HashMap<>(32);
        // ????????????
        HashMap<String, Double> yesterdayThree = new HashMap<>(16);
        // ????????????
        HashMap<String, Double> yesterdayFour = new HashMap<>(8);
        // ????????????
        HashMap<String, Double> yesterdayFive = new HashMap<>(4);
        // ????????????
        HashMap<String, Double> yesterdaySix = new HashMap<>(4);
        do {
            List<DailyRecord> records = dateListMap.get(sdf.format(date));
            if (CollectionUtils.isNotEmpty(records)) {
                // ????????????
                HashMap<String, Double> todayOne = new HashMap<>(64);
                // ????????????
                HashMap<String, Double> todayTwo = new HashMap<>(32);
                // ????????????
                HashMap<String, Double> todayThree = new HashMap<>(16);
                // ????????????
                HashMap<String, Double> todayFour = new HashMap<>(16);
                // ????????????
                HashMap<String, Double> todayFive = new HashMap<>(16);
                // ????????????
                HashMap<String, Double> todaySix = new HashMap<>(16);
                // ????????????
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
                            // ??????????????????????????????????????????7???
                            todaySeven.put(dailyRecord.getName(), v);
                        } else if (yesterdayFive.containsKey(name)) {
                            // ??????????????????????????????????????????6???
                            todaySix.put(dailyRecord.getName(), v);
                        } else if (yesterdayFour.containsKey(name)) {
                            // ??????????????????????????????????????????5???
                            todayFive.put(dailyRecord.getName(), v);
                        } else if (yesterdayThree.containsKey(name)) {
                            // ??????????????????????????????????????????4???
                            todayFour.put(dailyRecord.getName(), v);
                        } else if (yesterdayTwo.containsKey(name)) {
                            // ??????????????????????????????????????????3???
                            todayThree.put(dailyRecord.getName(), v);
                        } else if (yesterdayOne.containsKey(name)) {
                            // ???????????????????????????????????????2???
                            todayTwo.put(dailyRecord.getName(), v);
                        } else if (!yesterdayOne.containsKey(name)) {
                            // ??????????????????????????????
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
                    // ???????????????3-5???????????????????????????
                    log.warn("{}??????????????????" + "\n??????{}???:{}\n??????{}???:{}\n??????{}???:{}\n??????{}???:{}\n??????{}???:{}\n??????{}???:{}\n??????{}???:{}",
                            sdf.format(date), todayOne.keySet().size(), new ArrayList<>(todayOne.keySet()),
                            todayTwo.keySet().size(), new ArrayList<>(todayTwo.keySet()), todayThree.keySet().size(),
                            new ArrayList<>(todayThree.keySet()), todayFour.keySet().size(),
                            new ArrayList<>(todayFour.keySet()), todayFive.keySet().size(),
                            new ArrayList<>(todayFive.keySet()), todaySix.keySet().size(),
                            new ArrayList<>(todaySix.keySet()), todaySeven.keySet().size(),
                            new ArrayList<>(todaySeven.keySet()));
                    List<String> twoPlus = new ArrayList<>();
                    twoPlus.addAll(todayTwo.keySet());
                    twoPlus.addAll(todayThree.keySet());
                    twoPlus.addAll(todayFour.keySet());
                    twoPlus.addAll(todayFive.keySet());
                    twoPlus.addAll(todaySix.keySet());
                    twoPlus.addAll(todaySeven.keySet());
                    if (CollectionUtils.isNotEmpty(twoPlus)) {
                        List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>().in("name", twoPlus));
                        twoPlus.clear();
                        for (Stock stock : stocks) {
                            twoPlus.add(stock.getCode().substring(2, 8));
                        }
                        log.warn("????????????+??????:{}", StringUtil.collectionToString(twoPlus));
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
        log.warn("???????????????5??????+?????????:{}", StringUtil.collectionToString(fivePlankStock));
        log.warn("??????????????????????????????2???+?????????:{}", StringUtil.collectionToString(gemPlankStockTwice));
        log.error("??????>????????????????????????{}",
                (double) Math
                        .round(oneToTwo.values().stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100)
                        / 100);
        log.error("??????>????????????????????????{}",
                (double) Math
                        .round(twoToThree.values().stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100)
                        / 100);
        log.error("??????>????????????????????????{}",
                (double) Math
                        .round(threeToFour.values().stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100)
                        / 100);
        log.error("??????>????????????????????????{}",
                (double) Math
                        .round(fourToFive.values().stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100)
                        / 100);
        log.error("??????>????????????????????????{}",
                (double) Math
                        .round(fiveToSix.values().stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100)
                        / 100);
        log.error("??????>????????????????????????{}",
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
     * ????????????????????????????????????????????????????????????????????????????????????
     */
    public void barbarossa(Integer fundsPart, Long beginDay) {
        // ???????????????
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
            // ?????????
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
                            stockMapper.selectOne(new QueryWrapper<Stock>().eq("name", fundHoldingsTracking.getName()));
                    fundHoldingsTracking.setCode(stock.getCode());
                    fundHoldingsTracking.setQuarter(fundHoldingsParam.getQuarter());
                    List<DailyRecord> dailyRecordList = dailyRecordMapper.selectList(new QueryWrapper<DailyRecord>()
                            .eq("name", fundHoldingsTracking.getName()).ge("date", beginTime).le("date", endTime));
                    if (CollectionUtils.isEmpty(dailyRecordList)) {
                        HashMap<String, String> stockMap = new HashMap<>();
                        stockMap.put(stock.getCode(), stock.getName());
                        dailyRecordProcessor.run(stockMap);
                        Thread.sleep(60 * 1000);
                        dailyRecordList = dailyRecordMapper.selectList(new QueryWrapper<DailyRecord>()
                                .eq("name", fundHoldingsTracking.getName()).ge("date", beginTime).le("date", endTime));
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
                    log.warn("?????? [{}] {}?????????????????????????????????", stock.getName(), fundHoldingsParam.getQuarter());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * ?????? ??????+?????? ??????
     */
    private void updateForeignFundShareholding(Integer quarter) {
        HashMap<String, JSONObject> foreignShareholding = getForeignShareholding();
        List<ForeignFundHoldingsTracking> fundHoldings = fundHoldingsTrackingMapper
                .selectList(new QueryWrapper<ForeignFundHoldingsTracking>().eq("quarter", quarter));
        List<Stock> stocks = stockMapper.selectList(new QueryWrapper<>());
        if (CollectionUtils.isEmpty(foreignShareholding.values()) || CollectionUtils.isEmpty(fundHoldings)
                || CollectionUtils.isEmpty(stocks)) {
            return;
        }
        Map<String, Stock> stockMap = stocks.stream().collect(Collectors.toMap(Stock::getName, e -> e));
        for (ForeignFundHoldingsTracking tracking : fundHoldings) {
            JSONObject jsonObject = foreignShareholding.get(tracking.getName());
            try {
                if (Objects.nonNull(jsonObject)) {
                    long foreignTotalMarket = jsonObject.getLong("HOLD_MARKETCAP_CHG10") / W;
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
        log.warn("?????????????????????????????????????????????");
    }

    /**
     * ???????????????????????? ???????????????
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
