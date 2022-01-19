package com.mistra.plank.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2022/1/18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockSample implements Comparable<StockSample> {

    private String name;

    private double increase;

    @Override
    public int compareTo(StockSample o) {
        return (int) (this.increase * 100) - (int) (o.increase * 100);
    }
}
