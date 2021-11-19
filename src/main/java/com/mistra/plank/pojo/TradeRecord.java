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
     * 持股数量
     */
    @TableField(value = "number")
    private Integer number;

    /**
     * 价格
     */
    @TableField(value = "price")
    private BigDecimal price;

    /**
     * 利润
     */
    @TableField(value = "profit")
    private BigDecimal profit;

    /**
     * 日期
     */
    @TableField(value = "date")
    private Date date;

    public TradeRecord(Long id, String name, String code, Integer type, Integer number, BigDecimal price, BigDecimal profit, Date date) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.type = type;
        this.number = number;
        this.price = price;
        this.profit = profit;
        this.date = date;
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

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
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

    public BigDecimal getProfit() {
        return profit;
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
