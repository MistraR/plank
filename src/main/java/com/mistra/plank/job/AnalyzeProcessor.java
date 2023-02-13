package com.mistra.plank.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.config.SystemConstant;
import com.mistra.plank.dao.DailyRecordMapper;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.model.dto.StockMainFundSample;
import com.mistra.plank.model.dto.UpwardTrendSample;
import com.mistra.plank.model.entity.DailyRecord;
import com.mistra.plank.model.entity.Stock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.mistra.plank.common.util.StringUtil.collectionToString;

/**
 * @author rui.wang
 * @ Version: 1.0
 * @ Time: 2023/2/13 17:21
 * @ Description:
 */
@Slf4j
@Component
public class AnalyzeProcessor {

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private final StockMapper stockMapper;

    private final DailyRecordMapper dailyRecordMapper;

    private final PlankConfig plankConfig;

    public AnalyzeProcessor(StockMapper stockMapper, DailyRecordMapper dailyRecordMapper, PlankConfig plankConfig) {
        this.stockMapper = stockMapper;
        this.dailyRecordMapper = dailyRecordMapper;
        this.plankConfig = plankConfig;
    }

    public void analyzeMainFund() {
        log.warn("3|5|10日主力净流入>3亿:" + collectionToString(Barbarossa.mainFundDataAll.parallelStream()
                .filter(e -> e.getF267() > SystemConstant.TRANSACTION_AMOUNT_FILTER || e.getF164() > SystemConstant.TRANSACTION_AMOUNT_FILTER
                        || e.getF174() > SystemConstant.TRANSACTION_AMOUNT_FILTER)
                .map(plankConfig.getPrintName() ? StockMainFundSample::getF14 : StockMainFundSample::getF12)
                .collect(Collectors.toSet())));
    }

    /**
     * 找出周均线向上发散，上升趋势的股票
     * 周均线MA3>MA5>MA10>MA20
     * 上市不足20个交易日的次新股就不计算了
     * 趋势股选出来之后我一般会直接用.txt文档导入到东方财富windows版客户端，再来人为筛选一遍k线好看的票
     */
    public void analyzeUpwardTrend() {
        List<UpwardTrendSample> samples = new ArrayList<>(Barbarossa.STOCK_MAP.size());
        List<String> failed = new ArrayList<>();
        for (Map.Entry<String, String> entry : Barbarossa.STOCK_MAP.entrySet()) {
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
     * 分析最近一个月各连板晋级率
     */
    public void analyzePlank() {
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
}
