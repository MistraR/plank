package com.mistra.plank.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.dao.DailyRecordMapper;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.model.dto.StockMainFundSample;
import com.mistra.plank.model.entity.DailyRecord;
import com.mistra.plank.model.entity.Stock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
                .filter(e -> e.getF267() > plankConfig.getStockTurnoverFilter() || e.getF164() > plankConfig.getStockTurnoverFilter()
                        || e.getF174() > plankConfig.getStockTurnoverFilter())
                .map(plankConfig.getPrintName() ? StockMainFundSample::getF14 : StockMainFundSample::getF12)
                .collect(Collectors.toSet())));
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
                    if ((!code.contains("SZ30") && v > 9.6) || (code.contains("SZ30") && v > 19.6)) {
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
                if (date.after(DateUtils.addDays(new Date(), -2))) {
                    // 只打印最近2个交易日的连板数据
                    log.warn("{}日连板数据：" + "\n一板{}支:{}\n二板{}支:{}\n三板{}支:{}\n四板{}支:{}\n五板{}支:{}\n六板{}支:{}\n七板{}支:{}",
                            sdf.format(date), todayOne.keySet().size(), new ArrayList<>(todayOne.keySet()),
                            todayTwo.keySet().size(), new ArrayList<>(todayTwo.keySet()), todayThree.keySet().size(),
                            new ArrayList<>(todayThree.keySet()), todayFour.keySet().size(),
                            new ArrayList<>(todayFour.keySet()), todayFive.keySet().size(),
                            new ArrayList<>(todayFive.keySet()), todaySix.keySet().size(),
                            new ArrayList<>(todaySix.keySet()), todaySeven.keySet().size(),
                            new ArrayList<>(todaySeven.keySet()));
                    if (DateUtils.isSameDay(new Date(), date)) {
                        updateStock(todayOne.keySet(), 1);
                        updateStock(todayTwo.keySet(), 2);
                        updateStock(todayThree.keySet(), 3);
                        updateStock(todayFour.keySet(), 4);
                        updateStock(todayFive.keySet(), 5);
                        updateStock(todaySix.keySet(), 6);
                        updateStock(todaySeven.keySet(), 7);
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
        log.error("一板>一进二平均胜率：{}", (double) Math.round(oneToTwo.values().stream()
                .collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100) / 100);
        log.error("二板>二进三平均胜率：{}", (double) Math.round(twoToThree.values().stream()
                .collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100) / 100);
        log.error("三板>三进四平均胜率：{}", (double) Math.round(threeToFour.values().stream()
                .collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100) / 100);
        log.error("四板>四进五平均胜率：{}", (double) Math.round(fourToFive.values().stream()
                .collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100) / 100);
        log.error("五板>五进六平均胜率：{}", (double) Math.round(fiveToSix.values().stream()
                .collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100) / 100);
        log.error("六板>六进七平均胜率：{}", (double) Math.round(sixToSeven.values().stream()
                .collect(Collectors.averagingDouble(BigDecimal::doubleValue)) * 100) / 100);
    }

    private void updateStock(Set<String> names, int plankNumber) {
        if (CollectionUtils.isNotEmpty(names)) {
            LambdaUpdateWrapper<Stock> wrapper = new LambdaUpdateWrapper<Stock>().in(Stock::getName, names);
            stockMapper.update(Stock.builder().plankNumber(plankNumber).build(), wrapper);
        }
    }

    /**
     * 计算晋级率
     */
    private void promotion(HashMap<String, BigDecimal> promotion, HashMap<String, Double> today,
                           HashMap<String, Double> yesterday, Date date) {
        if (yesterday.size() > 0) {
            promotion.put(sdf.format(date), divide(today.size(), yesterday.size()));
        }
    }

    private BigDecimal divide(double x, int y) {
        return y <= 0 ? new BigDecimal(0) : new BigDecimal(x).divide(new BigDecimal(y), 2, RoundingMode.HALF_UP);
    }

}
