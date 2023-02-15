package com.mistra.plank.model.param;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author rui.wang
 * @ Version: 1.0
 * @ Time: 2022/11/16 11:43
 * @ Description:
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AutoTradeParam {

    /**
     * 股票名称
     */
    private String name;
    /**
     * 自动交易类型
     */
    private String automaticTradingType;

    /**
     * 买入数量
     */
    private Integer buyAmount;

    /**
     * 触发自动买入的价格
     */
    private BigDecimal suckTriggerPrice;

}
