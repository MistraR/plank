package com.mistra.plank.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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

    private String code;

    private String name;

    private Double highestPrice;

    private Double lowestPrice;

    /**
     * 实时价格
     */
    private Double currentPrice;

    /**
     * 建仓价 每日更新股票会更新为当日的MA10均价
     */
    private BigDecimal purchasePrice;

    /**
     * 距离建仓价比率
     */
    private int purchaseRate;

    /**
     * 当前涨幅
     */
    private Double increaseRate;

    /**
     * 今日主力流入
     */
    private Long mainFund;

    /**
     * 当前是否涨停
     */
    private boolean isPlank;

    /**
     * 跌停价
     */
    private Double limitDown;

    /**
     * 涨停价
     */
    private Double limitUp;

    /**
     * 市值
     */
    private BigDecimal market;

    /**
     * 成交量
     */
    private Long volume;

    /**
     * 成交额
     */
    private BigDecimal transactionAmount;

    @Override
    public int compareTo(StockRealTimePrice o) {
        return (int) ((o.increaseRate * 100) - (this.increaseRate * 100));
    }
}
