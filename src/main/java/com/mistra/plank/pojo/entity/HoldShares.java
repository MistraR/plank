package com.mistra.plank.pojo.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
     * 当前可用数量
     */
    @TableField(value = "number")
    private Integer number;

    /**
     * 当前成本价
     */
    @TableField(value = "cost")
    private BigDecimal cost;

    /**
     * 今日收盘价
     */
    @TableField(value = "current_price")
    private BigDecimal currentPrice;

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
