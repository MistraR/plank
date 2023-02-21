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
 * 描述
 *
 * @author mistra@future.com
 * @date 2021/11/18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "daily_record", autoResultMap = true)
public class DailyRecord {

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
     * 开盘价
     */
    @TableField(value = "open_price")
    private BigDecimal openPrice;

    /**
     * 收盘价
     */
    @TableField(value = "close_price")
    private BigDecimal closePrice;

    /**
     * 最高价
     */
    @TableField(value = "highest")
    private BigDecimal highest;

    /**
     * 最低价
     */
    @TableField(value = "lowest")
    private BigDecimal lowest;

    /**
     * 成交额(万)
     */
    @TableField(value = "amount")
    private Long amount;

    /**
     * 涨跌比率
     */
    @TableField(value = "increase_rate")
    private BigDecimal increaseRate;

    /**
     * 日期
     */
    @TableField(value = "date")
    private Date date;

    /**
     * 当日是否涨停收盘
     */
    @TableField(value = "plank")
    private Boolean plank;

}
