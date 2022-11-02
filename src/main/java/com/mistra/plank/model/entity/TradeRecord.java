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
 * 描述
 *
 * @author mistra@future.com
 * @date 2021/11/18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "trade_record", autoResultMap = true)
public class TradeRecord {

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
     * 交易类型0-买入1-卖出
     */
    @TableField(value = "type")
    private Integer type;

    /**
     * 数量
     */
    @TableField(value = "number")
    private Integer number;

    /**
     * 价格
     */
    @TableField(value = "price")
    private BigDecimal price;

    /**
     * 交易金额
     */
    @TableField(value = "money")
    private Integer money;

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
     * 日期
     */
    @TableField(value = "date")
    private Date date;

    /**
     * 交易原因
     */
    @TableField(value = "reason")
    private String reason;
}
