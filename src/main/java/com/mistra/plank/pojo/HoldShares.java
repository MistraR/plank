package com.mistra.plank.pojo;

import java.math.BigDecimal;

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
     * 持股数量
     */
    @TableField(value = "number")
    private Integer number;

    /**
     * 成本价
     */
    @TableField(value = "cost")
    private BigDecimal cost;

    /**
     * 当前价
     */
    @TableField(value = "current_price")
    private BigDecimal currentPrice;

    /**
     * 盈亏比率
     */
    @TableField(value = "rate")
    private BigDecimal rate;

    public HoldShares(Long id, String name, String code, Integer number, BigDecimal cost, BigDecimal currentPrice, BigDecimal rate) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.number = number;
        this.cost = cost;
        this.currentPrice = currentPrice;
        this.rate = rate;
    }

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
}
