package com.mistra.plank.job;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.OptionalDouble;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mistra.plank.config.PlankConfig;
import com.mistra.plank.mapper.ClearanceMapper;
import com.mistra.plank.mapper.DailyRecordMapper;
import com.mistra.plank.mapper.HoldSharesMapper;
import com.mistra.plank.mapper.StockMapper;
import com.mistra.plank.mapper.TradeRecordMapper;
import com.mistra.plank.pojo.Clearance;
import com.mistra.plank.pojo.DailyRecord;
import com.mistra.plank.pojo.HoldShares;
import com.mistra.plank.pojo.Stock;
import com.mistra.plank.pojo.TradeRecord;
import com.mistra.plank.pojo.enums.ClearanceReasonEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2021/11/19
 */
@Slf4j
@Component
public class Spider implements CommandLineRunner {

    private final StockMapper stockMapper;
    private final DailyRecordMapper dailyRecordMapper;
    private final ClearanceMapper clearanceMapper;
    private final TradeRecordMapper tradeRecordMapper;
    private final HoldSharesMapper holdSharesMapper;
    private final PlankConfig plankConfig;
    private final DailyRecordProcessor dailyRecordProcessor;
    private final DragonListProcessor dragonListProcessor;

    public static final HashMap<String, String> STOCK_MAP = new HashMap<>();

    /**
     * 总金额
     */
    public static BigDecimal BALANCE = new BigDecimal(1000000);
    /**
     * 可用金额
     */
    public static BigDecimal BALANCE_AVAILABLE = new BigDecimal(1000000);

    public Spider(StockMapper stockMapper, DailyRecordMapper dailyRecordMapper, ClearanceMapper clearanceMapper,
                  TradeRecordMapper tradeRecordMapper, HoldSharesMapper holdSharesMapper,
                  PlankConfig plankConfig, DailyRecordProcessor dailyRecordProcessor,
                  DragonListProcessor dragonListProcessor) {
        this.stockMapper = stockMapper;
        this.dailyRecordMapper = dailyRecordMapper;
        this.clearanceMapper = clearanceMapper;
        this.tradeRecordMapper = tradeRecordMapper;
        this.holdSharesMapper = holdSharesMapper;
        this.plankConfig = plankConfig;
        this.dailyRecordProcessor = dailyRecordProcessor;
        this.dragonListProcessor = dragonListProcessor;
    }

    @Override
    public void run(String... args) throws Exception {
        List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>()
                .notLike("name", "%ST%")
                .notLike("name", "%st%")
                .notLike("name", "%A%")
                .notLike("name", "%C%")
                .notLike("name", "%N%")
                .notLike("name", "%U%")
                .notLike("name", "%W%")
                .notLike("code", "%BJ%")
                .notLike("code", "%688%")
        );
        stocks.forEach(stock -> STOCK_MAP.put(stock.getCode(), stock.getName()));
        BALANCE = new BigDecimal(plankConfig.getFunds());
    }

    /**
     * 巴巴罗萨 工作日
     */
    @Scheduled(cron = "0 0 18 * * ? ")
    private void collect() throws Exception {
        Date date = DateUtils.addMinutes(new Date(plankConfig.getBeginDay()), 10);
        do {
            this.barbarossa(date);
            date = DateUtils.addDays(date, 1);
        } while (date.getTime() < System.currentTimeMillis());
    }

    private void barbarossa(Date date) {
        int week = DateUtil.dayOfWeek(date);
        if (week < 7 && week > 1) {
            List<String> stocks = checkStock(date);
            if (checkBalance()) {
                buyStock(stocks, date);
                sellStock(date);
            }
        }
    }

    /**
     * 检查可以买的票
     *
     * @return List<String>
     */
    private List<String> checkStock(Date date) {
        List<String> result = new ArrayList<>();


        return result;
    }

    private boolean checkBalance() {
        return BALANCE_AVAILABLE.intValue() > 10000;
    }

    private void buyStock(List<String> stocks, Date date) {

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
                if (!DateUtils.isSameDay(holdShare.getBuyTime(), date)) {
                    Page<DailyRecord> selectPage = dailyRecordMapper.selectPage(new Page<>(1, 20), new QueryWrapper<DailyRecord>()
                            .eq("code", holdShare.getCode())
                            .ge("date", DateUtils.addDays(date, -plankConfig.getDeficitMovingAverage()))
                            .le("date", date)
                            .orderByDesc("date"));
                    DailyRecord todayRecord = selectPage.getRecords().get(0);
                    OptionalDouble average = selectPage.getRecords().stream().mapToDouble(dailyRecord -> dailyRecord.getClosePrice().doubleValue()).average();
                    if (todayRecord.getLowest().doubleValue() < average.getAsDouble()) {
                        // 跌破均线，清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.BREAK_POSITION, date, average.getAsDouble());
                        continue;
                    }
                    // 盘中最低收益率
                    double profitLowRatio = todayRecord.getLowest().divide(holdShare.getBuyPrice(), 2).doubleValue() - 1;
                    if (profitLowRatio < plankConfig.getDeficitRatio().doubleValue()) {
                        // 跌破止损线，清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.BREAK_LOSS_LINE, date,
                                holdShare.getBuyPrice().doubleValue() * (1 + plankConfig.getDeficitRatio().doubleValue()));
                        continue;
                    }
                    if (holdShare.getFifteenProfit() && profitLowRatio <= 0.1) {
                        // 收益回撤到10个点止盈清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.TAKE_PROFIT, date, holdShare.getBuyPrice().doubleValue() * 1.1);
                        continue;
                    }
                    // 盘中最高收益
                    double profitHighRatio = todayRecord.getHighest().divide(holdShare.getBuyPrice(), 2).doubleValue() - 1;
                    if (profitHighRatio >= plankConfig.getProfitHalfRatio().doubleValue()) {
                        // 收益15% 减半仓
                        this.reduceStock(holdShare, ClearanceReasonEnum.PROFIT_HALF, date, todayRecord,
                                holdShare.getBuyPrice().doubleValue() * (1 + plankConfig.getProfitHalfRatio().doubleValue()));
                    }
                    if (profitHighRatio >= plankConfig.getProfitRatio().doubleValue()) {
                        // 收益到顶清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.PROFIT_UPPER, date,
                                holdShare.getCost().doubleValue() * (1 + plankConfig.getProfitRatio().doubleValue()));
                    }
                }
            }
        }
    }

    /**
     * 减仓股票
     *
     * @param holdShare           持仓记录
     * @param clearanceReasonEnum 清仓原因
     * @param date                时间
     * @param sellPrice           清仓价格
     */
    private void reduceStock(HoldShares holdShare, ClearanceReasonEnum clearanceReasonEnum, Date date, DailyRecord todayRecord, double sellPrice) {
        // 卖出数量
        int number = holdShare.getNumber() / 2;
        // 卖出金额
        double money = number * sellPrice;
        // 利润
        double profit = holdShare.getNumber() * (sellPrice - holdShare.getBuyPrice().doubleValue());
        // 总资产
        BALANCE = new BigDecimal(BALANCE.doubleValue() + number * sellPrice);
        TradeRecord tradeRecord = new TradeRecord();
        tradeRecord.setName(holdShare.getName());
        tradeRecord.setCode(holdShare.getCode());
        tradeRecord.setDate(date);
        tradeRecord.setMoney((int) money);
        tradeRecord.setReason(clearanceReasonEnum.name());
        tradeRecord.setBalance(BALANCE);
        tradeRecord.setPrice(new BigDecimal(sellPrice));
        tradeRecord.setNumber(number);
        tradeRecord.setType(1);
        tradeRecordMapper.insert(tradeRecord);
        holdShare.setNumber(number);
        holdShare.setCost(new BigDecimal(holdShare.getBuyPrice().doubleValue() - (profit / number)));
        holdShare.setFifteenProfit(true);
        holdShare.setCurrentPrice(todayRecord.getClosePrice());
        holdSharesMapper.updateById(holdShare);
    }

    /**
     * 清仓股票
     *
     * @param holdShare           持仓记录
     * @param clearanceReasonEnum 清仓原因
     * @param date                时间
     * @param sellPrice           清仓价格
     */
    private void clearanceStock(HoldShares holdShare, ClearanceReasonEnum clearanceReasonEnum, Date date, double sellPrice) {
        // 盈利
        double profit = holdShare.getNumber() * (sellPrice - holdShare.getCost().doubleValue());
        // 总资产
        BALANCE = new BigDecimal(BALANCE.doubleValue() + holdShare.getNumber() * sellPrice);
        TradeRecord tradeRecord = new TradeRecord();
        tradeRecord.setName(holdShare.getName());
        tradeRecord.setCode(holdShare.getCode());
        tradeRecord.setDate(date);
        tradeRecord.setMoney((int) (holdShare.getNumber() * sellPrice));
        tradeRecord.setReason(clearanceReasonEnum.name());
        tradeRecord.setBalance(BALANCE);
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
        clearance.setRate(new BigDecimal(profit / (holdShare.getBuyNumber() * holdShare.getBuyPrice().doubleValue())));
        clearance.setProfit(new BigDecimal(profit));
        clearance.setReason(clearanceReasonEnum.name());
        clearance.setDate(date);
        clearance.setBalance(BALANCE);
        clearance.setDayNumber(Days.daysBetween(new LocalDate(System.currentTimeMillis()), new LocalDate(holdShare.getBuyTime().getTime())).getDays());
        clearanceMapper.insert(clearance);
        holdSharesMapper.delete(new QueryWrapper<HoldShares>().eq("id", holdShare.getId()));
    }

}
