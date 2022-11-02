package com.mistra.plank.model.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2022/6/11
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpwardTrendSample implements Comparable<UpwardTrendSample> {

    private String name;

    private String code;

    /**
     * 3周均线
     */
    private BigDecimal ma3;

    /**
     * 5周均线
     */
    private BigDecimal ma5;

    /**
     * 10周均线
     */
    private BigDecimal ma10;

    /**
     * 20周均线
     */
    private BigDecimal ma20;

    /**
     * 方差
     */
    private double variance;

    @Override
    public int compareTo(UpwardTrendSample o) {
        return (int)(o.variance * 100) - (int)(this.variance * 100);
    }
}
