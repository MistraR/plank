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

    private Double todayRealTimePrice;

    private BigDecimal purchasePrice;

    private int rate;

    private Double increaseRate;

    /**
     * 今日主力流入
     */
    private Long mainFund;

    @Override
    public int compareTo(StockRealTimePrice o) {
        return (int)((o.increaseRate * 100) - (this.increaseRate * 100));
    }
}
