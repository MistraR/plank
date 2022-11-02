package com.mistra.plank.service.impl;

import static com.mistra.plank.job.Barbarossa.BALANCE;
import static com.mistra.plank.job.Barbarossa.BALANCE_AVAILABLE;
import static com.mistra.plank.job.Barbarossa.W;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.dao.ClearanceMapper;
import com.mistra.plank.dao.DailyRecordMapper;
import com.mistra.plank.dao.HoldSharesMapper;
import com.mistra.plank.dao.TradeRecordMapper;
import com.mistra.plank.model.entity.Clearance;
import com.mistra.plank.model.entity.DailyRecord;
import com.mistra.plank.model.entity.HoldShares;
import com.mistra.plank.model.entity.Stock;
import com.mistra.plank.model.entity.TradeRecord;
import com.mistra.plank.model.enums.ClearanceReasonEnum;
import com.mistra.plank.service.Plank;

import lombok.extern.slf4j.Slf4j;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2022/6/14
 */
@Slf4j
@Component
public class MistraPlank implements Plank {

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private final DailyRecordMapper dailyRecordMapper;
    private final ClearanceMapper clearanceMapper;
    private final TradeRecordMapper tradeRecordMapper;
    private final PlankConfig plankConfig;
    private final HoldSharesMapper holdSharesMapper;
    private final ScreeningStocks screeningStocks;

    public MistraPlank(DailyRecordMapper dailyRecordMapper, ClearanceMapper clearanceMapper,
        TradeRecordMapper tradeRecordMapper, PlankConfig plankConfig, HoldSharesMapper holdSharesMapper,
        ScreeningStocks screeningStocks) {
        this.dailyRecordMapper = dailyRecordMapper;
        this.clearanceMapper = clearanceMapper;
        this.tradeRecordMapper = tradeRecordMapper;
        this.plankConfig = plankConfig;
        this.holdSharesMapper = holdSharesMapper;
        this.screeningStocks = screeningStocks;
    }

    @Override
    public List<Stock> checkStock(Date date) {
        switch (plankConfig.getBuyStrategyEnum()) {
            case DRAGON_LIST:
                return screeningStocks.checkDragonListStock(date);
            case EXPLOSIVE_VOLUME_BACK:
                return screeningStocks.explosiveVolumeBack(date);
            case RED_THREE_SOLDIERS:
            default:
                return screeningStocks.checkRedThreeSoldiersStock(DateUtils.addDays(date, -1));
        }
    }

    @Override
    public void buyStock(List<Stock> stocks, Date date, Integer fundsPart) {
        for (Stock stock : stocks) {
            List<HoldShares> holdShares = holdSharesMapper.selectList(new QueryWrapper<>());
            if (holdShares.size() >= fundsPart) {
                log.info("仓位已打满！无法开新仓！");
                return;
            }
            Page<DailyRecord> selectPage = dailyRecordMapper.selectPage(new Page<>(1, 5),
                new QueryWrapper<DailyRecord>().eq("code", stock.getCode()).ge("date", DateUtils.addDays(date, -1))
                    .le("date", DateUtils.addDays(date, 12)).orderByAsc("date"));
            if (selectPage.getRecords().size() < 2) {
                continue;
            }
            DailyRecord dailyRecord = selectPage.getRecords().get(1);
            double openRatio =
                (selectPage.getRecords().get(1).getOpenPrice().subtract(selectPage.getRecords().get(0).getClosePrice()))
                    .divide(selectPage.getRecords().get(0).getClosePrice(), 2, RoundingMode.HALF_UP).doubleValue();
            if (openRatio > -0.03 && openRatio < plankConfig.getBuyPlankRatioLimit().doubleValue()
                && BALANCE_AVAILABLE.intValue() > W) {
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
     * @param date 开盘日期
     */
    @Override
    public void sellStock(Date date) {
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
}
