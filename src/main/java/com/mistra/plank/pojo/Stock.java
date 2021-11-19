package com.mistra.plank.pojo;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2021/11/18
 */
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
     * 当日成交量
     */
    @TableField(value = "volume")
    private Long volume;

    /**
     * 最近更新日期
     */
    @TableField(value = "modify_time")
    private Date modifyTime;

    public Stock(String code, String name, Long marketValue, Long volume, Date modifyTime) {
        this.code = code;
        this.name = name;
        this.marketValue = marketValue;
        this.volume = volume;
        this.modifyTime = modifyTime;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(Long marketValue) {
        this.marketValue = marketValue;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public Date getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Date modifyTime) {
        this.modifyTime = modifyTime;
    }
}
