package com.mistra.plank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.common.util.StringUtil;
import com.mistra.plank.dao.DailyRecordMapper;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.job.Barbarossa;
import com.mistra.plank.model.dto.UpwardTrendSample;
import com.mistra.plank.model.entity.DailyRecord;
import com.mistra.plank.model.entity.Stock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.mistra.plank.common.util.StringUtil.collectionToString;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2022/6/15
 */
@Slf4j
@Component
public class ScreeningStocks {

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private final DailyRecordMapper dailyRecordMapper;
    private final StockMapper stockMapper;
    private final PlankConfig plankConfig;

    public ScreeningStocks(DailyRecordMapper dailyRecordMapper, StockMapper stockMapper, PlankConfig plankConfig) {
        this.dailyRecordMapper = dailyRecordMapper;
        this.stockMapper = stockMapper;
        this.plankConfig = plankConfig;
    }

    /**
     * 找出日k均线多头排列的股票
     */
    public void movingAverageRise() {
        List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                .ge(Stock::getMa5, 0).ge(Stock::getTransactionAmount, plankConfig.getStockTurnoverFilter()));
        List<String> list = stocks.stream().filter(stock -> stock.getMa5().compareTo(stock.getMa10()) > 0
                        && stock.getMa10().compareTo(stock.getMa20()) > 0).map(stock -> StringUtils.substring(stock.getCode(), 2, 8))
                .collect(Collectors.toList());
        log.warn("日k均线多头排列:{}", collectionToString(list));
    }

    /**
     * 找出最近5天或10天 爆量回踩的票
     * 最高成交量>MA10*1.5 最低成交量<MA10*0.7 最低成交量出现在最高成交量之后 最低成交量那天的收盘价>最高成交量那天(收盘价+开盘价)/2
     * 最高成交额>10亿 最低成交额>5亿
     */
    public void explosiveVolumeBack() {
        Date date = new Date();
        List<Stock> result = new ArrayList<>();
        List<DailyRecord> dailyRecords = dailyRecordMapper
                .selectList(new LambdaQueryWrapper<DailyRecord>().ge(DailyRecord::getDate, DateUtils.addDays(date, -30))
                        .le(DailyRecord::getDate, date).orderByDesc(DailyRecord::getDate));
        Map<String, List<DailyRecord>> dailyRecordMap =
                dailyRecords.stream().collect(Collectors.groupingBy(DailyRecord::getCode));
        for (Map.Entry<String, List<DailyRecord>> entry : dailyRecordMap.entrySet()) {
            List<DailyRecord> recordList = entry.getValue();
            if (recordList.size() >= 10 && recordList.get(0).getAmount() > 30000) {
                Stock stock = explosiveVolumeBack(recordList.subList(0, 5), entry.getKey());
                if (Objects.nonNull(stock)) {
                    result.add(stock);
                } else {
                    stock = explosiveVolumeBack(recordList.subList(0, 10), entry.getKey());
                    if (Objects.nonNull(stock)) {
                        result.add(stock);
                    }
                }
            }
        }
        Collections.sort(result);
        log.warn("{}爆量回踩股票[{}]支:{}", sdf.format(date), result.size(), StringUtil.collectionToString(result.stream()
                .map(plankConfig.getPrintName() ? Stock::getName : Stock::getCode).collect(Collectors.toList())));
    }

    private Stock explosiveVolumeBack(List<DailyRecord> recordList, String code) {
        // 计算MA10
        double ma10 = recordList.stream().collect(Collectors.averagingLong(DailyRecord::getAmount));
        DailyRecord high = recordList.get(0), low = recordList.get(0);
        for (DailyRecord record : recordList) {
            if (high.getAmount() < record.getAmount()) {
                high = record;
            }
            if (low.getAmount() > record.getAmount()) {
                low = record;
            }
        }
        if (high.getClosePrice().compareTo(high.getOpenPrice()) > 0 && high.getDate().before(low.getDate())
                && high.getAmount() > Math.max(ma10 * 1.5, 100000) && low.getAmount() < ma10 * 0.7
                && low.getAmount() > 50000 && low.getClosePrice()
                .doubleValue() > ((high.getClosePrice().doubleValue() + high.getOpenPrice().doubleValue()) * 0.5)) {
            Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getCode, code));
            if (Objects.nonNull(stock.getMa20()) && recordList.get(0).getClosePrice().compareTo(stock.getMa20()) > 0) {
                stock.setCode(StringUtils.substring(stock.getCode(), 2, 8));
                return stock;
            }
        }
        return null;
    }

    /**
     * 找出周均线向上发散，上升趋势的股票
     * 周均线MA3>MA5>MA10>MA20
     * 上市不足20个交易日的次新股就不计算了
     * 趋势股选出来之后我一般会直接用.txt文档导入到东方财富windows版客户端，再来人为筛选一遍k线好看的票
     */
    public void upwardTrend() {
        List<UpwardTrendSample> samples = new ArrayList<>(Barbarossa.STOCK_ALL_MAP.size());
        List<String> failed = new ArrayList<>();
        for (Map.Entry<String, String> entry : Barbarossa.STOCK_ALL_MAP.entrySet()) {
            Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getCode, entry.getKey()));
            if (stock.getTrack() || stock.getTransactionAmount().doubleValue() < plankConfig.getStockTurnoverFilter()) {
                continue;
            }
            List<DailyRecord> dailyRecords = dailyRecordMapper.selectList(new LambdaQueryWrapper<DailyRecord>()
                    .eq(DailyRecord::getCode, entry.getKey())
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
                double v = variance(new double[]{ma3, ma5, ma10, ma20});
                samples.add(UpwardTrendSample.builder().ma3(new BigDecimal(ma3).setScale(2, RoundingMode.HALF_UP))
                        .ma5(new BigDecimal(ma5).setScale(2, RoundingMode.HALF_UP))
                        .ma10(new BigDecimal(ma10).setScale(2, RoundingMode.HALF_UP))
                        .ma20(new BigDecimal(ma20).setScale(2, RoundingMode.HALF_UP)).name(entry.getValue())
                        .code(StringUtils.substring(entry.getKey(), 2, 8)).variance(v).build());
            }
        }
        if (CollectionUtils.isNotEmpty(failed)) {
//            log.error("{}的交易数据不完整(可能是次新股，上市不足100个交易日)", collectionToString(failed));
        }
        Collections.sort(samples);
        log.warn("上升趋势的股票一共[{}]支:{}", samples.size(), collectionToString(samples.stream().map(plankConfig.getPrintName() ?
                UpwardTrendSample::getName : UpwardTrendSample::getCode).collect(Collectors.toSet())));
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
