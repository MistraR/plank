package com.mistra.plank.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主力流入
 *
 * @author mistra@future.com
 * @date 2022/2/8
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockMainFundSample implements Comparable<StockMainFundSample> {

    /**
     * f12 code
     */
    private String f12;

    /**
     * f14 名称
     */
    private String f14;

    /**
     * 今日主力净流入
     */
    private Long f62;

    /**
     * 今日涨跌
     */
    private Double f3;

    /**
     * 今日净流入占比
     */
    private Double f184;

    /**
     * 3日主力净流入
     */
    private Long f267;

    /**
     * 3日涨跌
     */
    private Double f127;

    /**
     * 3日净流入占比
     */
    private Double f268;

    /**
     * 5日主力净流入
     */
    private Long f164;

    /**
     * 5日涨跌
     */
    private Double f109;

    /**
     * 5日净流入占比
     */
    private Double f165;

    /**
     * 10日主力净流入
     */
    private Long f174;

    /**
     * 10日涨跌
     */
    private Double f160;

    /**
     * 10日净流入占比
     */
    private Double f175;

    @Override
    public int compareTo(StockMainFundSample o) {
        return (int)((o.f62 / 10000) - (this.f62 / 10000));
    }
}
