package com.mistra.plank.pojo.entity;

import java.math.BigDecimal;
import java.util.Date;

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
@TableName(value = "stock", autoResultMap = true)
public class Stock {

    @TableId(value = "code")
    private String code;

    @TableField
    private String name;

    /**
     * 市值
     */
    @TableField(value = "market_value")
    private Long marketValue;

    /**
     * 当前价格
     */
    @TableField(value = "current_price")
    private BigDecimal currentPrice;

    /**
     * 预计建仓价格
     */
    @TableField(value = "purchase_price")
    private BigDecimal purchasePrice;

    /**
     * 当日成交量（手）
     */
    @TableField(value = "volume")
    private Long volume;

    /**
     * 当日成交额
     */
    @TableField(value = "transaction_amount")
    private BigDecimal transactionAmount;

    /**
     * 最近更新日期
     */
    @TableField(value = "modify_time")
    private Date modifyTime;

    /**
     * 是否关注
     */
    @TableField(value = "track")
    private Boolean track;

    /**
     * 是否持仓
     */
    @TableField(value = "shareholding")
    private Boolean shareholding;

    /**
     * 所属板块
     */
    @TableField(value = "classification")
    private String classification;
}
