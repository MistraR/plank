package com.mistra.plank.pojo;

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
 * 清仓交割单
 *
 * @author mistra@future.com
 * @date 2021/11/19
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "clearance", autoResultMap = true)
public class Clearance {

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
     * 买入成本价
     */
    @TableField(value = "cost_price")
    private BigDecimal costPrice;

    /**
     * 建仓股数
     */
    @TableField(value = "number")
    private Integer number;

    /**
     * 清仓价格
     */
    @TableField(value = "price")
    private BigDecimal price;

    /**
     * 盈亏比率
     */
    @TableField(value = "rate")
    private BigDecimal rate;

    /**
     * 利润
     */
    @TableField(value = "profit")
    private BigDecimal profit;

    /**
     * 账户余额
     */
    @TableField(value = "balance")
    private BigDecimal balance;

    /**
     * 可用余额
     */
    @TableField(value = "available_balance")
    private BigDecimal availableBalance;

    /**
     * 清仓原因
     */
    @TableField(value = "reason")
    private String reason;

    /**
     * 日期
     */
    @TableField(value = "date")
    private Date date;

    /**
     * 持股天数
     */
    @TableField(value = "day_number")
    private Integer dayNumber;
}
