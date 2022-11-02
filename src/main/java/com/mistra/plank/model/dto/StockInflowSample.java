package com.mistra.plank.model.dto;

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
public class StockInflowSample implements Comparable<StockInflowSample> {

    private String name;

    private double money;

    @Override
    public int compareTo(StockInflowSample o) {
        return (int)(this.money) - (int)(o.money);
    }
}
