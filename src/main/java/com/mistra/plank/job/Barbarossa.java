package com.mistra.plank.job;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mistra.plank.config.PlankConfig;
import com.mistra.plank.mapper.ClearanceMapper;
import com.mistra.plank.mapper.DailyRecordMapper;
import com.mistra.plank.mapper.DragonListMapper;
import com.mistra.plank.mapper.HoldSharesMapper;
import com.mistra.plank.mapper.StockMapper;
import com.mistra.plank.mapper.TradeRecordMapper;
import com.mistra.plank.pojo.Clearance;
import com.mistra.plank.pojo.DailyRecord;
import com.mistra.plank.pojo.DragonList;
import com.mistra.plank.pojo.HoldShares;
import com.mistra.plank.pojo.Stock;
import com.mistra.plank.pojo.TradeRecord;
import com.mistra.plank.pojo.dto.StockRealTimePrice;
import com.mistra.plank.pojo.enums.ClearanceReasonEnum;

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

    private final ExecutorService executorService = new ThreadPoolExecutor(10, 20, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(5000), new NamedThreadFactory("DailyRecord线程-", false));

    public static final HashMap<String, String> STOCK_MAP = new HashMap<>();

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
        DragonListMapper dragonListMapper, PlankConfig plankConfig, DailyRecordProcessor dailyRecordProcessor) {
        this.stockMapper = stockMapper;
        this.stockProcessor = stockProcessor;
        this.dailyRecordMapper = dailyRecordMapper;
        this.clearanceMapper = clearanceMapper;
        this.tradeRecordMapper = tradeRecordMapper;
        this.holdSharesMapper = holdSharesMapper;
        this.dragonListMapper = dragonListMapper;
        this.plankConfig = plankConfig;
        this.dailyRecordProcessor = dailyRecordProcessor;
    }

    @Override
    public void run(String... args) {
        List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>().notLike("name", "%ST%")
            .notLike("name", "%st%").notLike("name", "%A%").notLike("name", "%C%").notLike("name", "%N%")
            .notLike("name", "%U%").notLike("name", "%W%").notLike("code", "%BJ%").notLike("code", "%688%"));
        stocks.forEach(stock -> STOCK_MAP.put(stock.getCode(), stock.getName()));
        log.info("一共加载[{}]支股票！", stocks.size());
        BALANCE = new BigDecimal(plankConfig.getFunds());
        BALANCE_AVAILABLE = BALANCE;
        if (DateUtil.hour(new Date(), true) >= 15) {
            // 15点后读取当日交易数据
            dailyRecordProcessor.run(Barbarossa.STOCK_MAP);
            // 更新每只股票收盘价
            stockProcessor.run();
            log.info("今日交易数据更新成功，开始分析连板数据!");
            // 分析连板数据
            analyze();
        } else {
            // 15点以前实时监控涨跌
            monitor(plankConfig.getMonitor());
        }
    }

    /**
     * 补充写入今日交易数据
     */
    public void replenish() {
        List<DailyRecord> stocks =
            dailyRecordMapper.selectList(new QueryWrapper<DailyRecord>().ge("date", DateUtils.addDays(new Date(), -1)));
        for (DailyRecord stock : stocks) {
            Barbarossa.STOCK_MAP.remove(stock.getCode());
        }
        dailyRecordProcessor.run(Barbarossa.STOCK_MAP);
    }

    /**
     * 实时监测数据 显示股票实时涨跌幅度，最高，最低价格
     *
     * @param haveStock haveStock
     */
    public void monitor(String haveStock) {
        executorService.submit(() -> {
            try {
                List<String> haveStockList = Arrays.asList(haveStock.split(","));
                List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>().in("name", haveStockList));
                List<StockRealTimePrice> realTimePrices = new ArrayList<>();
                while (DateUtil.hour(new Date(), true) <= 15 && DateUtil.hour(new Date(), true) >= 9) {
                    for (Stock stock : stocks) {
                        String url = plankConfig.getXueQiuStockDetailUrl();
                        url = url.replace("{code}", stock.getCode())
                            .replace("{time}", String.valueOf(System.currentTimeMillis()))
                            .replace("{recentDayNumber}", "1");
                        DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
                        HttpGet httpGet = new HttpGet(URI.create(url));
                        httpGet.setHeader("Cookie", plankConfig.getXueQiuCookie());
                        CloseableHttpResponse response = defaultHttpClient.execute(httpGet);
                        HttpEntity entity = response.getEntity();
                        String body = "";
                        if (entity != null) {
                            body = EntityUtils.toString(entity, "UTF-8");
                        }
                        JSONObject data = JSON.parseObject(body).getJSONObject("data");
                        JSONArray list = data.getJSONArray("item");
                        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(list)) {
                            for (Object o : list) {
                                double v = ((JSONArray)o).getDoubleValue(5);
                                double rate =
                                    -(double)Math.round(((v - stock.getPurchasePrice().doubleValue()) / v) * 100) / 100;
                                realTimePrices.add(StockRealTimePrice.builder().todayRealTimePrice(v)
                                    .name(stock.getName()).todayHighestPrice(((JSONArray)o).getDoubleValue(3))
                                    .todayLowestPrice(((JSONArray)o).getDoubleValue(4))
                                    .purchasePrice(stock.getPurchasePrice()).rate((int)(rate * 100))
                                    .increaseRate(((JSONArray)o).getDoubleValue(7)).build());
                            }
                        }
                    }
                    Collections.sort(realTimePrices);
                    log.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    for (StockRealTimePrice realTimePrice : realTimePrices) {
                        log.info(realTimePrice.getName() + (realTimePrice.getName().length() == 3 ? "  " : "") + ": 高:"
                            + realTimePrice.getTodayHighestPrice() + " | 低:" + realTimePrice.getTodayLowestPrice()
                            + " | 建仓价:" + realTimePrice.getPurchasePrice() + " | 现价:"
                            + realTimePrice.getTodayRealTimePrice() + " | 距离建仓价百分比:" + realTimePrice.getRate()
                            + "% | 涨幅:" + realTimePrice.getIncreaseRate());
                    }
                    realTimePrices.clear();
                    Thread.sleep(10000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 分析最近一个月各连板晋级率
     */
    public void analyze() {
        // 4连板+的股票
        HashSet<String> fourPlankStock = new HashSet<>();
        HashMap<String, Integer> gemPlankStockNumber = new HashMap<>();
        Date date = new DateTime(DateUtils.addDays(new Date(), -30)).withHourOfDay(0).withMinuteOfHour(0)
            .withSecondOfMinute(0).withMillisOfSecond(0).toDate();
        // 首板一进二胜率
        HashMap<String, BigDecimal> oneToTwo = new HashMap<>(64);
        // 二板二进三胜率
        HashMap<String, BigDecimal> twoToThree = new HashMap<>(64);
        // 三板三进四胜率
        HashMap<String, BigDecimal> threeToFour = new HashMap<>(64);
        // 四板四进五胜率
        HashMap<String, BigDecimal> fourToFive = new HashMap<>(64);
        // 五板五进六胜率
        HashMap<String, BigDecimal> fiveToSix = new HashMap<>(64);
        // 六板六进七胜率
        HashMap<String, BigDecimal> sixToSeven = new HashMap<>(64);
        List<DailyRecord> dailyRecords = dailyRecordMapper.selectList(new QueryWrapper<DailyRecord>().ge("date", date));
        Map<String, List<DailyRecord>> dateListMap =
            dailyRecords.stream().collect(Collectors.groupingBy(dailyRecord -> sdf.format(dailyRecord.getDate())));
        // 昨日首板
        HashMap<String, Double> yesterdayOne = new HashMap<>(64);
        // 昨日二板
        HashMap<String, Double> yesterdayTwo = new HashMap<>(64);
        // 昨日三板
        HashMap<String, Double> yesterdayThree = new HashMap<>(64);
        // 昨日四板
        HashMap<String, Double> yesterdayFour = new HashMap<>(64);
        // 昨日五板
        HashMap<String, Double> yesterdayFive = new HashMap<>(64);
        // 昨日六板
        HashMap<String, Double> yesterdaySix = new HashMap<>(64);
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
                fourPlankStock.addAll(todayFour.keySet());
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
        log.info("最近一个月4连板+的股票:{}", fourPlankStock.toString().replace(" ", "").replace("[", ",").replace("]", ""));
        log.info("最近一个月创业板涨停2次+的股票:{}",
            gemPlankStockTwice.toString().replace(" ", "").replace("[", ",").replace("]", ""));
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
    public void barbarossa() {
        // 清除老数据
        holdSharesMapper.delete(new QueryWrapper<>());
        clearanceMapper.delete(new QueryWrapper<>());
        Date date = new Date(plankConfig.getBeginDay());
        do {
            this.barbarossa(date);
            date = DateUtils.addDays(date, 1);
        } while (date.getTime() < System.currentTimeMillis());
    }

    private void barbarossa(Date date) {
        int week = DateUtil.dayOfWeek(date);
        if (week < 7 && week > 1) {
            // 工作日
            List<Stock> stocks = this.checkCanBuyStock(date);
            if (CollectionUtils.isNotEmpty(stocks) && BALANCE_AVAILABLE.intValue() > 10000) {
                this.buyStock(stocks, date);
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

    private void buyStock(List<Stock> stocks, Date date) {
        for (Stock stock : stocks) {
            List<HoldShares> holdShares = holdSharesMapper.selectList(new QueryWrapper<>());
            if (holdShares.size() >= plankConfig.getFundsPart()) {
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
                    int money = BALANCE.intValue() / plankConfig.getFundsPart();
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
                    tradeRecord.setReason("买入" + holdShare.getName() + number * 100 + "股，花费" + cost + "元，当前可用余额"
                        + BALANCE_AVAILABLE.intValue());
                    tradeRecord.setBalance(BALANCE.setScale(2, RoundingMode.HALF_UP));
                    tradeRecord.setAvailableBalance(BALANCE_AVAILABLE.setScale(2, RoundingMode.HALF_UP));
                    tradeRecord.setPrice(dailyRecord.getOpenPrice());
                    tradeRecord.setNumber(number * 100);
                    tradeRecord.setType(0);
                    tradeRecordMapper.insert(tradeRecord);
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
                        // 收益回撤到10个点止盈清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.TAKE_PROFIT, date,
                            holdShare.getBuyPrice().doubleValue() * 1.1);
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
        // 本次卖出部分盈利金额
        BigDecimal profit = new BigDecimal(number * (sellPrice - holdShare.getBuyPrice().doubleValue()));
        // 可用金额
        BALANCE_AVAILABLE = BALANCE_AVAILABLE.add(new BigDecimal(money));
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
        log.info("{}日减仓 {},目前盈利 {} 元!", sdf.format(date), holdShare.getName(),
            holdShare.getProfit().add(profit).intValue());
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
        // 本次卖出部分盈利金额
        BigDecimal profit =
            BigDecimal.valueOf(holdShare.getNumber() * (sellPrice - holdShare.getBuyPrice().doubleValue()));
        // 总盈利
        profit = holdShare.getProfit().add(profit);
        // 总资产
        BALANCE = BALANCE.add(profit);
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
        log.info("{}日清仓 {},总共盈利 {} 元!当前总资产: {} ", sdf.format(date), holdShare.getName(), profit.intValue(),
            BALANCE.intValue());
    }
}
