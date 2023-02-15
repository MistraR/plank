package com.mistra.plank.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 持仓
 *
 * @author mistra@future.com
 * @date 2021/11/18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "hold_shares", autoResultMap = true)
public class HoldShares {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField
    private String name;

    /**
     * 证券代码
     */
    @TableField
    private String code;

    /**
     * HoldSharesEnum
     */
    @TableField(value = "type")
    private String type;

    /**
     * 自动交易类型 AutomaticTradingEnum
     */
    @TableField(value = "automatic_trading_type")
    private String automaticTradingType;

    /**
     * 触发止盈价格
     */
    @TableField(value = "take_profit_price")
    private BigDecimal takeProfitPrice;

    /**
     * 触发止损价格
     */
    @TableField(value = "stop_loss_price")
    private BigDecimal stopLossPrice;

    /**
     * 持股数量
     */
    @TableField(value = "number")
    private Integer number;

    /**
     * 当前可用数量
     */
    @TableField(value = "available_volume")
    private Integer availableVolume;

    /**
     * 盈亏比率
     */
    @TableField(value = "rate")
    private BigDecimal rate;

    /**
     * 建仓成本价
     */
    @TableField(value = "buy_price")
    private BigDecimal buyPrice;

    /**
     * 利润
     */
    @TableField(value = "profit")
    private BigDecimal profit;

    /**
     * 建仓数量
     */
    @TableField(value = "buy_number")
    private Integer buyNumber;

    /**
     * 收益是否到过15%
     */
    @TableField(value = "fifteen_profit")
    private Boolean fifteenProfit;

    /**
     * 建仓日期
     */
    @TableField(value = "buy_time")
    private Date buyTime;

}
