package com.mistra.plank.pojo;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 持仓
 *
 * @author mistra@future.com
 * @date 2021/11/18
 */
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

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(BigDecimal buyPrice) {
        this.buyPrice = buyPrice;
    }

    public Date getBuyTime() {
        return buyTime;
    }

    public void setBuyTime(Date buyTime) {
        this.buyTime = buyTime;
    }

    public Boolean getFifteenProfit() {
        return fifteenProfit;
    }

    public void setFifteenProfit(Boolean fifteenProfit) {
        this.fifteenProfit = fifteenProfit;
    }

    public Integer getBuyNumber() {
        return buyNumber;
    }

    public void setBuyNumber(Integer buyNumber) {
        this.buyNumber = buyNumber;
    }
}
