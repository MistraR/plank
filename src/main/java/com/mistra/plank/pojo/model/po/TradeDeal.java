package com.mistra.plank.pojo.model.po;

import java.math.BigDecimal;
import java.util.Date;

public class TradeDeal extends BaseModel {

    private static final long serialVersionUID = 1L;

    private String stockCode;
    private String dealCode;
    private BigDecimal price;
    private int volume;
    private String tradeType;
    private String crTradeType;
    private Date tradeTime;

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getDealCode() {
        return dealCode;
    }

    public void setDealCode(String dealCode) {
        this.dealCode = dealCode;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public String getCrTradeType() {
        return crTradeType;
    }

    public void setCrTradeType(String crTradeType) {
        this.crTradeType = crTradeType;
    }

    public Date getTradeTime() {
        return tradeTime;
    }

    public void setTradeTime(Date tradeTime) {
        this.tradeTime = tradeTime;
    }

}
