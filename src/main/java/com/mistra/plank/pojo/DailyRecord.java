package com.mistra.plank.pojo;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2021/11/18
 */
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
     * 成交额
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
    }

    public BigDecimal getIncreaseRate() {
        return increaseRate;
    }

    public void setIncreaseRate(BigDecimal increaseRate) {
        this.increaseRate = increaseRate;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public BigDecimal getHighest() {
        return highest;
    }

    public void setHighest(BigDecimal highest) {
        this.highest = highest;
    }

    public BigDecimal getLowest() {
        return lowest;
    }

    public void setLowest(BigDecimal lowest) {
        this.lowest = lowest;
    }
}
