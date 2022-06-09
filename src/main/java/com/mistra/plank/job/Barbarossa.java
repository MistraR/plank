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
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mistra.plank.config.PlankConfig;
import com.mistra.plank.mapper.ClearanceMapper;
import com.mistra.plank.mapper.DailyRecordMapper;
import com.mistra.plank.mapper.DragonListMapper;
import com.mistra.plank.mapper.FundHoldingsTrackingMapper;
import com.mistra.plank.mapper.HoldSharesMapper;
import com.mistra.plank.mapper.StockMapper;
import com.mistra.plank.mapper.TradeRecordMapper;
import com.mistra.plank.pojo.dto.StockMainFundSample;
import com.mistra.plank.pojo.dto.StockRealTimePrice;
import com.mistra.plank.pojo.entity.Clearance;
import com.mistra.plank.pojo.entity.DailyRecord;
import com.mistra.plank.pojo.entity.DragonList;
import com.mistra.plank.pojo.entity.ForeignFundHoldingsTracking;
import com.mistra.plank.pojo.entity.HoldShares;
import com.mistra.plank.pojo.entity.Stock;
import com.mistra.plank.pojo.entity.TradeRecord;
import com.mistra.plank.pojo.enums.ClearanceReasonEnum;
import com.mistra.plank.pojo.param.FundHoldingsParam;
import com.mistra.plank.util.HttpUtil;
import com.mistra.plank.util.UploadDataListener;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * 巴巴罗萨计划
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
    private final DragonListMapper dragonListMapper;
    private final PlankConfig plankConfig;
    private final DailyRecordProcessor dailyRecordProcessor;
    private final FundHoldingsTrackingMapper fundHoldingsTrackingMapper;

    private final ExecutorService executorService = new ThreadPoolExecutor(10, 20, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(5000), new NamedThreadFactory("T", false));
    // 主力趋势流入 过滤金额 >3亿
    private final Integer mainFundFilterAmount = 300000000;
    // 所有股票 key-code value-name
    public static final HashMap<String, String> STOCK_MAP = new HashMap<>();
    // 需要监控关注的票 key-name value-Stock
    public static final HashMap<String, Stock> TRACK_STOCK_MAP = new HashMap<>();

    public static final CopyOnWriteArrayList<StockMainFundSample> mainFundData = new CopyOnWriteArrayList<>();
    public static final ConcurrentHashMap<String, StockMainFundSample> mainFundDataMap = new ConcurrentHashMap<>(6400);

    /**
     * 总金额
     */
    public static BigDecimal BALANCE = new BigDecimal(1000000);
    /**
     * 可用金额
     */
    public static BigDecimal BALANCE_AVAILABLE = new BigDecimal(1000000);

    public Barbarossa(StockMapper stockMapper, StockProcessor stockProcessor, DailyRecordMapper dailyRecordMapper,
        ClearanceMapper clearanceMapper, TradeRecordMapper tradeRecordMapper, HoldSharesMapper holdSharesMapper,
        DragonListMapper dragonListMapper, PlankConfig plankConfig, DailyRecordProcessor dailyRecordProcessor,
        FundHoldingsTrackingMapper fundHoldingsTrackingMapper) {
        this.stockMapper = stockMapper;
        this.stockProcessor = stockProcessor;
        this.dailyRecordMapper = dailyRecordMapper;
        this.clearanceMapper = clearanceMapper;
        this.tradeRecordMapper = tradeRecordMapper;
        this.holdSharesMapper = holdSharesMapper;
        this.dragonListMapper = dragonListMapper;
        this.plankConfig = plankConfig;
        this.dailyRecordProcessor = dailyRecordProcessor;
        this.fundHoldingsTrackingMapper = fundHoldingsTrackingMapper;
    }

    @Override
    public void run(String... args) {
        List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>().notLike("name", "%ST%")
            .notLike("name", "%st%").notLike("name", "%A%").notLike("name", "%C%").notLike("name", "%N%")
            .notLike("name", "%U%").notLike("name", "%W%").notLike("code", "%BJ%").notLike("code", "%688%"));
        stocks.forEach(stock -> STOCK_MAP.put(stock.getCode(), stock.getName()));
        stocks.stream().filter(e -> e.getShareholding() || e.getTrack())
            .forEach(stock -> TRACK_STOCK_MAP.put(stock.getName(), stock));
        log.warn("一共加载[{}]支股票！", stocks.size());
        log.warn("一共加载[{}]支监控股票！", TRACK_STOCK_MAP.size());
        BALANCE = new BigDecimal(plankConfig.getFunds());
        BALANCE_AVAILABLE = new BigDecimal(plankConfig.getFunds());
        if (DateUtil.hour(new Date(), true) >= 15) {
            // 15点后读取当日交易数据
            dailyRecordProcessor.run(Barbarossa.STOCK_MAP);
            // 更新每只股票收盘价，MA5 MA10 MA20
            stockProcessor.run();
            // 更新 外资+基金 持仓 只更新到最新一季度报告的汇总表上 基金季报有滞后性，外资持仓则是实时的
            updateForeignFundShareholding(202201);
            // 分析连板数据
            analyze();
        } else {
            // 15点以前实时监控涨跌
            monitor();
        }
    }

    /**
     * 实时监测数据 显示股票实时涨跌幅度，最高，最低价格
     *
     * 想要监测哪些股票需要手动在数据库stock表更改track字段为true
     *
     */
    public void monitor() {
        executorService.submit(() -> {
            try {
                List<StockRealTimePrice> realTimePrices = new ArrayList<>();
                while (DateUtil.hour(new Date(), true) <= 15 && DateUtil.hour(new Date(), true) >= 9) {
                    List<Stock> stocks = stockMapper
                        .selectList(new QueryWrapper<Stock>().eq("track", true).or().eq("shareholding", true));
                    Map<String, Stock> stockMap = stocks.stream().collect(Collectors.toMap(Stock::getName, e -> e));
                    for (Stock stock : stocks) {
                        List<DailyRecord> dailyRecords = dailyRecordMapper
                            .selectPage(new Page<>(1, 9),
                                new QueryWrapper<DailyRecord>().eq("code", stock.getCode()).orderByDesc("date"))
                            .getRecords();
                        String url = plankConfig.getXueQiuStockDetailUrl().replace("{code}", stock.getCode())
                            .replace("{time}", String.valueOf(System.currentTimeMillis()))
                            .replace("{recentDayNumber}", "1");
                        String body = HttpUtil.getHttpGetResponseString(url, plankConfig.getXueQiuCookie());
                        JSONObject data = JSON.parseObject(body).getJSONObject("data");
                        JSONArray list = data.getJSONArray("item");
                        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(list)) {
                            for (Object o : list) {
                                double v = ((JSONArray)o).getDoubleValue(5);
                                double ma10Rate, ma5Rate = 0;
                                BigDecimal ma5Price = new BigDecimal(0);
                                if (dailyRecords.size() >= 4) {
                                    List<BigDecimal> collect = dailyRecords.subList(0, 4).stream()
                                        .map(DailyRecord::getClosePrice).collect(Collectors.toList());
                                    collect.add(new BigDecimal(v));
                                    double ma5 =
                                        collect.stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue));
                                    ma5Rate = (double)Math.round(((ma5 - v) / v) * 100) / 100;
                                    ma5Price = new BigDecimal(ma5).setScale(2, RoundingMode.HALF_UP);
                                }
                                BigDecimal ma10Price = new BigDecimal(0);
                                if (dailyRecords.size() >= 9) {
                                    List<BigDecimal> collect = dailyRecords.stream().map(DailyRecord::getClosePrice)
                                        .collect(Collectors.toList());
                                    collect.add(new BigDecimal(v));
                                    double ma10 =
                                        collect.stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue));
                                    ma10Rate = (double)Math.round(((ma10 - v) / v) * 100) / 100;
                                    ma10Price = new BigDecimal(ma10).setScale(2, RoundingMode.HALF_UP);
                                } else {
                                    ma10Rate =
                                        (double)Math.round(((stock.getPurchasePrice().doubleValue() - v) / v) * 100)
                                            / 100;
                                }
                                realTimePrices.add(StockRealTimePrice.builder().todayRealTimePrice(v)
                                    .name(stock.getName()).todayHighestPrice(((JSONArray)o).getDoubleValue(3))
                                    .todayLowestPrice(((JSONArray)o).getDoubleValue(4)).ma10(ma10Price).ma5(ma5Price)
                                    .mainFund(mainFundDataMap.containsKey(stock.getName())
                                        ? mainFundDataMap.get(stock.getName()).getF62() / 10000 : 0)
                                    .purchasePrice(stock.getPurchasePrice()).ma10Rate((int)(ma10Rate * 100))
                                    .ma5Rate((int)(ma5Rate * 100)).increaseRate(((JSONArray)o).getDoubleValue(7))
                                    .build());
                            }
                        }
                    }
                    Collections.sort(mainFundData);
                    List<StockMainFundSample> mainFundSamplesTopTen =
                        mainFundData.size() > 10 ? mainFundData.subList(0, 10) : new ArrayList<>();
                    Collections.sort(realTimePrices);
                    System.out.println("\n\n\n");
                    log.error(
                        "--------------------------------------今日主力净流入前10---------------------------------------");
                    log.warn(mainFundSamplesTopTen.stream()
                        .map(e -> e.getF14() + "[" + e.getF62() / 10000 + "万]" + e.getF3() + "%|")
                        .collect(Collectors.toList()).toString().replace(" ", "").replace("[", "").replace("]", ""));
                    log.error(
                        "------------------------------------3|5|10日主力净流入>3亿-------------------------------------");
                    log.warn(mainFundData.parallelStream()
                        .filter(e -> e.getF267() > mainFundFilterAmount || e.getF164() > mainFundFilterAmount
                            || e.getF174() > mainFundFilterAmount)
                        .map(StockMainFundSample::getF14).collect(Collectors.toSet()).toString().replace(" ", "")
                        .replace("[", "").replace("]", ""));
                    log.error(
                        "---------------------------------------------持仓---------------------------------------------");
                    for (StockRealTimePrice realTimePrice : realTimePrices) {
                        if (stockMap.get(realTimePrice.getName()).getShareholding()) {
                            Barbarossa.log.warn(convertLog(realTimePrice));
                        }
                    }
                    realTimePrices.removeIf(e -> stockMap.get(e.getName()).getShareholding());
                    log.error(
                        "----------------------------------------接近建仓点(MA5)----------------------------------------");
                    for (StockRealTimePrice realTimePrice : realTimePrices) {
                        if (realTimePrice.getMa5Rate() >= -1 && realTimePrice.getMa5Rate() < 1) {
                            Barbarossa.log
                                .warn(convertLog(realTimePrice, realTimePrice.getMa5Rate(), realTimePrice.getMa5()));
                        }
                    }
                    log.error(
                        "----------------------------------------接近建仓点(MA10)----------------------------------------");
                    for (StockRealTimePrice realTimePrice : realTimePrices) {
                        if (realTimePrice.getMa10Rate() >= -3 && realTimePrice.getMa10Rate() < 3) {
                            Barbarossa.log
                                .warn(convertLog(realTimePrice, realTimePrice.getMa10Rate(), realTimePrice.getMa10()));
                        }
                    }
                    log.error(
                        "---------------------------------------------暴跌---------------------------------------------");
                    for (StockRealTimePrice realTimePrice : realTimePrices) {
                        if (realTimePrice.getIncreaseRate() < -5) {
                            Barbarossa.log.warn(convertLog(realTimePrice));
                        }
                    }
                    realTimePrices.clear();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        executorService.submit(this::queryMainFundData);
    }

    /**
     * 查询主力实时流入数据
     */
    private void queryMainFundData() {
        while (DateUtil.hour(new Date(), true) <= 15 && DateUtil.hour(new Date(), true) >= 9) {
            String body = HttpUtil.getHttpGetResponseString(plankConfig.getMainFundUrl(), null);
            JSONArray array = JSON.parseObject(body).getJSONObject("data").getJSONArray("diff");
            List<StockMainFundSample> result = new ArrayList<>();
            array.parallelStream().forEach(e -> {
                try {
                    StockMainFundSample mainFundSample =
                        JSONObject.parseObject(e.toString(), StockMainFundSample.class);
                    if (TRACK_STOCK_MAP.containsKey(mainFundSample.getF14())) {
                        result.add(mainFundSample);
                        mainFundDataMap.put(mainFundSample.getF14(), mainFundSample);
                    }
                } catch (Exception exception) {
                }
            });
            mainFundData.clear();
            mainFundData.addAll(result);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
    }

    private String convertLog(StockRealTimePrice realTimePrice, int rate, BigDecimal purchasePrice) {
        return realTimePrice.getName() + (realTimePrice.getName().length() == 3 ? "  " : "") + "[高:"
            + realTimePrice.getTodayHighestPrice() + " | 现:" + realTimePrice.getTodayRealTimePrice() + " | 低:"
            + realTimePrice.getTodayLowestPrice() + " | 建仓价:" + purchasePrice + " | 距离建仓价:" + rate + "% | 涨幅:"
            + realTimePrice.getIncreaseRate() + " | 主力流入:" + realTimePrice.getMainFund() + "万";
    }

    private String convertLog(StockRealTimePrice realTimePrice) {
        return realTimePrice.getName() + (realTimePrice.getName().length() == 3 ? "  " : "") + "[高:"
            + realTimePrice.getTodayHighestPrice() + " | 现:" + realTimePrice.getTodayRealTimePrice() + " | 低:"
            + realTimePrice.getTodayLowestPrice() + " | 建仓价:" + realTimePrice.getPurchasePrice() + " | 距离建仓价:"
            + realTimePrice.getMa10Rate() + "% | 涨幅:" + realTimePrice.getIncreaseRate() + " | 主力流入:"
            + realTimePrice.getMainFund() + "万";
    }

    /**
     * 分析最近一个月各连板晋级率
     */
    public void analyze() {
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
        List<DailyRecord> dailyRecords = dailyRecordMapper.selectList(new QueryWrapper<DailyRecord>().ge("date", date));
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
                        } else if (!yesterdayOne.containsKey(name)) {
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
                log.info(
                    "\n-------------------------------------------------------------------------------------------{}日-------------------------------------------------------------------------------------------"
                        + "\n一板{}支:{}\n二板{}支:{}\n三板{}支:{}\n四板{}支:{}\n五板{}支:{}\n六板{}支:{}\n七板{}支:{}"
                        + "\n--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------",
                    sdf.format(date), todayOne.keySet().size(), new ArrayList<>(todayOne.keySet()),
                    todayTwo.keySet().size(), new ArrayList<>(todayTwo.keySet()), todayThree.keySet().size(),
                    new ArrayList<>(todayThree.keySet()), todayFour.keySet().size(),
                    new ArrayList<>(todayFour.keySet()), todayFive.keySet().size(), new ArrayList<>(todayFive.keySet()),
                    todaySix.keySet().size(), new ArrayList<>(todaySix.keySet()), todaySeven.keySet().size(),
                    new ArrayList<>(todaySeven.keySet()));
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
        log.info("最近一个月5连板+的股票:{}", fivePlankStock.toString().replace(" ", "").replace("[", "").replace("]", ""));
        log.info("最近一个月创业板涨停2次+的股票:{}",
            gemPlankStockTwice.toString().replace(" ", "").replace("[", "").replace("]", ""));
        log.info("一板>一进二平均胜率：{}",
            (double)Math
                .round(oneToTwo.values().stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100)
                / 100);
        log.info("二板>二进三平均胜率：{}",
            (double)Math
                .round(twoToThree.values().stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100)
                / 100);
        log.info("三板>三进四平均胜率：{}",
            (double)Math
                .round(threeToFour.values().stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100)
                / 100);
        log.info("四板>四进五平均胜率：{}",
            (double)Math
                .round(fourToFive.values().stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100)
                / 100);
        log.info("五板>五进六平均胜率：{}",
            (double)Math
                .round(fiveToSix.values().stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100)
                / 100);
        log.info("六板>六进七平均胜率：{}",
            (double)Math
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
        BALANCE = new BigDecimal(1000000);
        BALANCE_AVAILABLE = new BigDecimal(1000000);
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
            List<Stock> stocks = this.checkCanBuyStock(date);
            if (CollectionUtils.isNotEmpty(stocks) && BALANCE_AVAILABLE.intValue() > 10000) {
                this.buyStock(stocks, date, fundsPart);
            }
            this.sellStock(date);
        }
    }

    /**
     * 检查可以买的票 首板或者2板 10日涨幅介于10-22% 计算前8天的振幅在15%以内
     *
     * @return List<String>
     */
    private List<Stock> checkCanBuyStock(Date date) {
        List<DragonList> dragonLists = dragonListMapper.selectList(new QueryWrapper<DragonList>().ge("date", date)
            .lt("date", DateUtils.addDays(date, 1)).ge("price", 5).le("price", 100).notLike("name", "%ST%")
            .notLike("name", "%st%").notLike("name", "%A%").notLike("name", "%C%").notLike("name", "%N%")
            .notLike("name", "%U%").notLike("name", "%W%").notLike("code", "%BJ%").notLike("code", "%688%"));
        if (CollectionUtils.isEmpty(dragonLists)) {
            return null;
        }
        List<DailyRecord> dailyRecords = new ArrayList<>();
        for (DragonList dragonList : dragonLists) {
            Page<DailyRecord> page = dailyRecordMapper.selectPage(new Page<>(1, 30),
                new QueryWrapper<DailyRecord>().eq("code", dragonList.getCode()).le("date", date)
                    .ge("date", DateUtils.addDays(date, -30)).orderByDesc("date"));
            if (page.getRecords().size() > 10) {
                dailyRecords.addAll(page.getRecords().subList(0, 10));
            }
        }
        Map<String, List<DailyRecord>> map = dailyRecords.stream().collect(Collectors.groupingBy(DailyRecord::getCode));
        List<String> stockCode = new ArrayList<>();
        for (Map.Entry<String, List<DailyRecord>> entry : map.entrySet()) {
            // 近8日涨幅
            BigDecimal eightRatio = entry.getValue().get(0).getClosePrice()
                .divide(entry.getValue().get(8).getClosePrice(), 2, RoundingMode.HALF_UP);
            // 近3日涨幅
            BigDecimal threeRatio = entry.getValue().get(0).getClosePrice()
                .divide(entry.getValue().get(3).getClosePrice(), 2, RoundingMode.HALF_UP);
            // 前3个交易日大跌的也排除
            if (eightRatio.doubleValue() <= 1.22 && eightRatio.doubleValue() >= 1.1 && threeRatio.doubleValue() < 1.22
                && entry.getValue().get(0).getIncreaseRate().doubleValue() > 0.04
                && entry.getValue().get(1).getIncreaseRate().doubleValue() > -0.04
                && entry.getValue().get(2).getIncreaseRate().doubleValue() > -0.04) {
                stockCode.add(entry.getKey());
            }
        }
        dragonLists = dragonLists.stream().filter(dragonList -> stockCode.contains(dragonList.getCode()))
            .collect(Collectors.toList());
        dragonLists =
            dragonLists.stream().sorted((a, b) -> b.getNetBuy().compareTo(a.getNetBuy())).collect(Collectors.toList());
        List<Stock> result = new ArrayList<>();
        for (DragonList dragonList : dragonLists) {
            result.add(stockMapper.selectOne(new QueryWrapper<Stock>().eq("code", dragonList.getCode())));
        }
        return result;
    }

    private void buyStock(List<Stock> stocks, Date date, Integer fundsPart) {
        for (Stock stock : stocks) {
            List<HoldShares> holdShares = holdSharesMapper.selectList(new QueryWrapper<>());
            if (holdShares.size() >= fundsPart) {
                log.info("仓位已打满！无法开新仓！");
                return;
            }
            Page<DailyRecord> selectPage = dailyRecordMapper.selectPage(new Page<>(1, 5),
                new QueryWrapper<DailyRecord>().eq("code", stock.getCode()).ge("date", date)
                    .le("date", DateUtils.addDays(date, 12)).orderByAsc("date"));
            if (selectPage.getRecords().size() < 2) {
                continue;
            }
            DailyRecord dailyRecord = selectPage.getRecords().get(1);
            double openRatio =
                (selectPage.getRecords().get(1).getOpenPrice().subtract(selectPage.getRecords().get(0).getClosePrice()))
                    .divide(selectPage.getRecords().get(0).getClosePrice(), 2, RoundingMode.HALF_UP).doubleValue();
            if (openRatio > -0.03 && openRatio < plankConfig.getBuyPlankRatioLimit().doubleValue()
                && BALANCE_AVAILABLE.intValue() > 10000) {
                // 低开2个点以下不买
                HoldShares one = holdSharesMapper.selectOne(new QueryWrapper<HoldShares>().eq("code", stock.getCode()));
                if (Objects.isNull(one)) {
                    int money = BALANCE.intValue() / fundsPart;
                    money = Math.min(money, BALANCE_AVAILABLE.intValue());
                    int number = money / dailyRecord.getOpenPrice().multiply(new BigDecimal(100)).intValue();
                    double cost = number * 100 * dailyRecord.getOpenPrice().doubleValue();
                    BALANCE_AVAILABLE = BALANCE_AVAILABLE.subtract(new BigDecimal(cost));
                    HoldShares holdShare = HoldShares.builder().buyTime(DateUtils.addHours(dailyRecord.getDate(), 9))
                        .code(stock.getCode()).name(stock.getName()).cost(dailyRecord.getOpenPrice())
                        .fifteenProfit(false).number(number * 100).profit(new BigDecimal(0))
                        .currentPrice(dailyRecord.getOpenPrice()).rate(new BigDecimal(0))
                        .buyPrice(dailyRecord.getOpenPrice()).buyNumber(number * 100).build();
                    holdSharesMapper.insert(holdShare);
                    TradeRecord tradeRecord = new TradeRecord();
                    tradeRecord.setName(holdShare.getName());
                    tradeRecord.setCode(holdShare.getCode());
                    tradeRecord.setDate(DateUtils.addHours(dailyRecord.getDate(), 9));
                    tradeRecord.setMoney((int)(number * 100 * dailyRecord.getOpenPrice().doubleValue()));
                    String note = "以" + dailyRecord.getOpenPrice().doubleValue() + "价格建仓" + holdShare.getName()
                        + number * 100 + "股，花费" + (int)cost + "，当前可用余额" + BALANCE_AVAILABLE.intValue();
                    tradeRecord.setReason(note);
                    tradeRecord.setBalance(BALANCE.setScale(2, RoundingMode.HALF_UP));
                    tradeRecord.setAvailableBalance(BALANCE_AVAILABLE.setScale(2, RoundingMode.HALF_UP));
                    tradeRecord.setPrice(dailyRecord.getOpenPrice());
                    tradeRecord.setNumber(number * 100);
                    tradeRecord.setType(0);
                    tradeRecordMapper.insert(tradeRecord);
                    log.warn("{}日建仓{}", sdf.format(date), note);
                }
            }
        }
    }

    /**
     * 减仓或清仓股票
     *
     * @param date 日期
     */
    private void sellStock(Date date) {
        List<HoldShares> holdShares = holdSharesMapper.selectList(new QueryWrapper<>());
        if (CollectionUtils.isNotEmpty(holdShares)) {
            for (HoldShares holdShare : holdShares) {
                if (!DateUtils.isSameDay(holdShare.getBuyTime(), date)
                    && holdShare.getBuyTime().getTime() < date.getTime()) {
                    Page<DailyRecord> selectPage = dailyRecordMapper.selectPage(new Page<>(1, 25),
                        new QueryWrapper<DailyRecord>().eq("code", holdShare.getCode())
                            .ge("date", DateUtils.addDays(date, -plankConfig.getDeficitMovingAverage() - 9))
                            .le("date", date).orderByDesc("date"));
                    // 今日数据明细
                    DailyRecord todayRecord = selectPage.getRecords().get(0);
                    List<DailyRecord> dailyRecords =
                        selectPage.getRecords().size() >= plankConfig.getDeficitMovingAverage()
                            ? selectPage.getRecords().subList(0, plankConfig.getDeficitMovingAverage() - 1)
                            : selectPage.getRecords();
                    // 止损均线价格
                    OptionalDouble average = dailyRecords.stream()
                        .mapToDouble(dailyRecord -> dailyRecord.getClosePrice().doubleValue()).average();
                    if (average.isPresent() && (todayRecord.getLowest().doubleValue() <= average.getAsDouble())) {
                        // 跌破均线，清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.BREAK_POSITION, date, average.getAsDouble());
                        continue;
                    }
                    // 盘中最低收益率
                    double profitLowRatio = todayRecord.getLowest().subtract(holdShare.getBuyPrice())
                        .divide(holdShare.getBuyPrice(), 2, RoundingMode.HALF_UP).doubleValue();
                    if (profitLowRatio < plankConfig.getDeficitRatio().doubleValue()) {
                        // 跌破止损线，清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.BREAK_LOSS_LINE, date,
                            holdShare.getBuyPrice().doubleValue() * (1 + plankConfig.getDeficitRatio().doubleValue()));
                        continue;
                    }
                    if (holdShare.getFifteenProfit()
                        && profitLowRatio <= plankConfig.getProfitClearanceRatio().doubleValue()) {
                        // 收益回撤到plankConfig.getProfitClearanceRatio()个点止盈清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.TAKE_PROFIT, date,
                            holdShare.getBuyPrice().doubleValue()
                                * plankConfig.getProfitClearanceRatio().doubleValue());
                        continue;
                    }
                    // 盘中最高收益率
                    double profitHighRatio = todayRecord.getHighest().subtract(holdShare.getBuyPrice())
                        .divide(holdShare.getBuyPrice(), 2, RoundingMode.HALF_UP).doubleValue();
                    if (profitHighRatio >= plankConfig.getProfitUpperRatio().doubleValue()) {
                        // 收益25% 清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.PROFIT_UPPER, date,
                            holdShare.getBuyPrice().doubleValue()
                                * (1 + plankConfig.getProfitUpperRatio().doubleValue()));
                    } else if (profitHighRatio >= plankConfig.getProfitQuarterRatio().doubleValue()) {
                        // 收益20% 减至1/4仓
                        this.reduceStock(holdShare, ClearanceReasonEnum.POSITION_QUARTER, date, todayRecord,
                            holdShare.getBuyPrice().doubleValue()
                                * (1 + plankConfig.getProfitQuarterRatio().doubleValue()));
                    } else if (profitHighRatio >= plankConfig.getProfitHalfRatio().doubleValue()) {
                        // 收益15% 减半仓
                        this.reduceStock(holdShare, ClearanceReasonEnum.POSITION_HALF, date, todayRecord,
                            holdShare.getBuyPrice().doubleValue()
                                * (1 + plankConfig.getProfitHalfRatio().doubleValue()));
                    }
                    // 持股超过x天 并且 收益不到20% 清仓
                    if (Days.daysBetween(new LocalDate(holdShare.getBuyTime().getTime()), new LocalDate(date.getTime()))
                        .getDays() > plankConfig.getClearanceDay()) {
                        this.clearanceStock(holdShare, ClearanceReasonEnum.TEN_DAY, date,
                            todayRecord.getOpenPrice().add(todayRecord.getClosePrice()).doubleValue() / 2);
                    }
                }
            }
        }
    }

    /**
     * 减仓股票
     *
     * @param holdShare 持仓记录
     * @param clearanceReasonEnum 清仓原因
     * @param date 时间
     * @param sellPrice 清仓价格
     */
    private void reduceStock(HoldShares holdShare, ClearanceReasonEnum clearanceReasonEnum, Date date,
        DailyRecord todayRecord, double sellPrice) {
        if (holdShare.getNumber() <= 0) {
            holdSharesMapper.delete(new QueryWrapper<HoldShares>().eq("id", holdShare.getId()));
            return;
        }
        // 卖出数量
        int number = holdShare.getNumber() <= 100 ? 100 : holdShare.getNumber() / 2;
        // 卖出金额
        double money = number * sellPrice;
        // 本次卖出部分盈亏金额
        BigDecimal profit = new BigDecimal(number * (sellPrice - holdShare.getBuyPrice().doubleValue()));
        // 可用金额
        BALANCE_AVAILABLE = BALANCE_AVAILABLE.add(new BigDecimal(money));
        // 总金额
        BALANCE = BALANCE.add(profit);
        TradeRecord tradeRecord = new TradeRecord();
        tradeRecord.setName(holdShare.getName());
        tradeRecord.setCode(holdShare.getCode());
        tradeRecord.setDate(date);
        tradeRecord.setMoney((int)money);
        tradeRecord.setReason("减仓" + holdShare.getName() + number + "股，卖出金额" + (int)money + "元，当前可用余额"
            + BALANCE_AVAILABLE.intValue() + "，减仓原因" + clearanceReasonEnum.getDesc());
        tradeRecord.setBalance(BALANCE.setScale(2, RoundingMode.HALF_UP));
        tradeRecord.setAvailableBalance(BALANCE_AVAILABLE.setScale(2, RoundingMode.HALF_UP));
        tradeRecord.setPrice(new BigDecimal(sellPrice));
        tradeRecord.setNumber(number);
        tradeRecord.setType(1);
        tradeRecordMapper.insert(tradeRecord);
        if (holdShare.getNumber() - number == 0) {
            holdSharesMapper.delete(new QueryWrapper<HoldShares>().eq("id", holdShare.getId()));
            return;
        }
        holdShare.setNumber(holdShare.getNumber() - number);
        holdShare.setCost(holdShare.getBuyPrice().multiply(new BigDecimal(holdShare.getBuyNumber())).subtract(profit)
            .divide(new BigDecimal(number), 2, RoundingMode.HALF_UP));
        holdShare.setProfit(holdShare.getProfit().add(profit));
        holdShare.setFifteenProfit(true);
        holdShare.setCurrentPrice(todayRecord.getClosePrice());
        holdShare.setRate(todayRecord.getClosePrice().subtract(holdShare.getBuyPrice()).divide(holdShare.getBuyPrice(),
            2, RoundingMode.HALF_UP));
        holdSharesMapper.updateById(holdShare);
        log.warn("{}日减仓 [{}],目前总盈利[{}] | 总金额 [{}] | 可用金额 [{}]", sdf.format(date), holdShare.getName(),
            holdShare.getProfit().add(profit).intValue(), BALANCE.intValue(), BALANCE_AVAILABLE.intValue());
    }

    /**
     * 清仓股票
     *
     * @param holdShare 持仓记录
     * @param clearanceReasonEnum 清仓原因
     * @param date 时间
     * @param sellPrice 清仓价格
     */
    private void clearanceStock(HoldShares holdShare, ClearanceReasonEnum clearanceReasonEnum, Date date,
        double sellPrice) {
        if (holdShare.getNumber() <= 0) {
            holdSharesMapper.delete(new QueryWrapper<HoldShares>().eq("id", holdShare.getId()));
            return;
        }
        // 卖出金额
        double money = holdShare.getNumber() * sellPrice;
        // 本次卖出部分盈亏金额
        BigDecimal profit =
            BigDecimal.valueOf(holdShare.getNumber() * (sellPrice - holdShare.getBuyPrice().doubleValue()));
        // 总资产
        BALANCE = BALANCE.add(profit);
        // 总盈利
        profit = holdShare.getProfit().add(profit);
        // 可用金额
        BALANCE_AVAILABLE = BALANCE_AVAILABLE.add(new BigDecimal(money));
        TradeRecord tradeRecord = new TradeRecord();
        tradeRecord.setName(holdShare.getName());
        tradeRecord.setCode(holdShare.getCode());
        tradeRecord.setDate(date);
        tradeRecord.setMoney((int)money);
        tradeRecord.setReason("清仓" + holdShare.getName() + holdShare.getNumber() + "股，卖出金额" + (int)money + "元，当前可用余额"
            + BALANCE_AVAILABLE.intValue() + "，清仓原因" + clearanceReasonEnum.getDesc());
        tradeRecord.setBalance(BALANCE.setScale(2, RoundingMode.HALF_UP));
        tradeRecord.setAvailableBalance(BALANCE_AVAILABLE.setScale(2, RoundingMode.HALF_UP));
        tradeRecord.setPrice(new BigDecimal(sellPrice));
        tradeRecord.setNumber(holdShare.getNumber());
        tradeRecord.setType(1);
        tradeRecordMapper.insert(tradeRecord);
        Clearance clearance = new Clearance();
        clearance.setCode(holdShare.getCode());
        clearance.setName(holdShare.getName());
        clearance.setCostPrice(holdShare.getBuyPrice());
        clearance.setNumber(holdShare.getBuyNumber());
        clearance.setPrice(new BigDecimal(sellPrice));
        clearance
            .setRate(profit.divide(BigDecimal.valueOf(holdShare.getBuyNumber() * holdShare.getBuyPrice().doubleValue()),
                2, RoundingMode.HALF_UP));
        clearance.setProfit(profit);
        clearance.setReason("清仓" + holdShare.getName() + "总计盈亏" + profit.intValue() + "元，清仓原因:"
            + clearanceReasonEnum.getDesc() + "，建仓日期" + sdf.format(holdShare.getBuyTime()));
        clearance.setDate(date);
        clearance.setBalance(BALANCE.setScale(2, RoundingMode.HALF_UP));
        clearance.setAvailableBalance(BALANCE_AVAILABLE.setScale(2, RoundingMode.HALF_UP));
        clearance.setDayNumber(
            Days.daysBetween(new LocalDate(holdShare.getBuyTime().getTime()), new LocalDate(date.getTime())).getDays());
        clearanceMapper.insert(clearance);
        holdSharesMapper.delete(new QueryWrapper<HoldShares>().eq("id", holdShare.getId()));
        log.warn("{}日清仓 [{}],目前总盈利[{}] | 总金额 [{}] | 可用金额 [{}]", sdf.format(date), holdShare.getName(),
            profit.intValue(), BALANCE.intValue(), BALANCE_AVAILABLE.intValue());
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
                    log.warn("更新 [{}] {}季报基金持仓数据完成！", stock.getName(), fundHoldingsParam.getQuarter());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * 更新 外资+基金 持仓
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
                    long foreignTotalMarket = jsonObject.getLong("HOLD_MARKETCAP_CHG10") / 10000;
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
        log.warn("更新外资最新持仓数据完成！");
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
                JSONArray array = JSON.parseObject(body).getJSONObject("result").getJSONArray("data");
                for (Object o : array) {
                    JSONObject jsonObject = (JSONObject)o;
                    result.put(jsonObject.getString("SECURITY_NAME"), jsonObject);
                }
                pageNumber++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
