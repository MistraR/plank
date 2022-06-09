package com.mistra.plank.pojo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2022/2/8
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockRealTimePrice implements Comparable<StockRealTimePrice> {

    private String name;

    private Double todayHighestPrice;

    private Double todayLowestPrice;

    /**
     * 实时价格
     */
    private Double todayRealTimePrice;

    /**
     * 建仓价 每日更新股票会更新为当日的MA10均价
     */
    private BigDecimal purchasePrice;

    /**
     * MA20
     */
    private BigDecimal ma20;

    /**
     * MA10
     */
    private BigDecimal ma10;

    /**
     * MA5
     */
    private BigDecimal ma5;

    /**
     * 距离MA20的比率
     */
    private int ma20Rate;

    /**
     * 距离MA10的比率
     */
    private int ma10Rate;

    /**
     * 距离MA5的比率
     */
    private int ma5Rate;

    /**
     * 当前涨幅
     */
    private Double increaseRate;

    /**
     * 今日主力流入
     */
    private Long mainFund;

    @Override
    public int compareTo(StockRealTimePrice o) {
        return (int)((o.ma10Rate * 100) - (this.ma10Rate * 100));
    }
}
