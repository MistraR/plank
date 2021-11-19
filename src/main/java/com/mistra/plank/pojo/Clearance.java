package com.mistra.plank.pojo;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 清仓交割单
 *
 * @author mistra@future.com
 * @date 2021/11/19
 */
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

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getProfit() {
        return profit;
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Integer getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
