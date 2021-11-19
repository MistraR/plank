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
@TableName(value = "dragon_list", autoResultMap = true)
public class DragonList {

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
     * 净买入(W)
     */
    @TableField(value = "net_buy")
    private Integer netBuy;

    /**
     * 买入前5合计(W)
     */
    @TableField(value = "first_five_net_buy")
    private Integer firstFiveNetBuy;

    /**
     * 卖出前5合计(W)
     */
    @TableField(value = "first_five_net_sell")
    private Integer firstFiveNetSell;

    /**
     * 最近5日涨跌幅
     */
    @TableField(value = "last_five_days_rate")
    private BigDecimal lastFiveDaysRate;

    /**
     * 日期
     */
    @TableField(value = "date")
    private Date date;

    public DragonList(Long id, String name, String code, Integer netBuy, Integer firstFiveNetBuy, Integer firstFiveNetSell, BigDecimal lastFiveDaysRate, Date date) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.netBuy = netBuy;
        this.firstFiveNetBuy = firstFiveNetBuy;
        this.firstFiveNetSell = firstFiveNetSell;
        this.lastFiveDaysRate = lastFiveDaysRate;
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

    public Integer getNetBuy() {
        return netBuy;
    }

    public void setNetBuy(Integer netBuy) {
        this.netBuy = netBuy;
    }

    public Integer getFirstFiveNetBuy() {
        return firstFiveNetBuy;
    }

    public void setFirstFiveNetBuy(Integer firstFiveNetBuy) {
        this.firstFiveNetBuy = firstFiveNetBuy;
    }

    public Integer getFirstFiveNetSell() {
        return firstFiveNetSell;
    }

    public void setFirstFiveNetSell(Integer firstFiveNetSell) {
        this.firstFiveNetSell = firstFiveNetSell;
    }

    public BigDecimal getLastFiveDaysRate() {
        return lastFiveDaysRate;
    }

    public void setLastFiveDaysRate(BigDecimal lastFiveDaysRate) {
        this.lastFiveDaysRate = lastFiveDaysRate;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
