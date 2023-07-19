package com.mistra.plank.model.entity;

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
     * 最高盈利百分比
     */
    @TableField(value = "highest_profit_ratio")
    private BigDecimal highestProfitRatio;

    /**
     * 今日是否触板,炸板则挂跌停走人
     */
    @TableField(value = "today_plank")
    private Boolean todayPlank;

    /**
     * 是否清仓
     */
    @TableField(value = "clearance")
    private Boolean clearance;

    /**
     * 清仓原因
     */
    @TableField(value = "clearance_reason")
    private String clearanceReason;

    /**
     * 建仓日期
     */
    @TableField(value = "buy_time")
    private Date buyTime;

    /**
     * 清仓日期
     */
    @TableField(value = "sale_time")
    private Date saleTime;

}
