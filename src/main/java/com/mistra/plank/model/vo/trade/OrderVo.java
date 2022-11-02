package com.mistra.plank.model.vo.trade;

import java.math.BigDecimal;

public class OrderVo {

    private String stockCode;
    private String stockName;
    private String abbreviation;
    private String tradeType;
    private String crTradeType;
    private String entrustCode;
    private String ensuerTime;
    private BigDecimal price;
    private int volume;
    private String state;

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
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

    public String getEntrustCode() {
        return entrustCode;
    }

    public void setEntrustCode(String entrustCode) {
        this.entrustCode = entrustCode;
    }

    public String getEnsuerTime() {
        return ensuerTime;
    }

    public void setEnsuerTime(String ensuerTime) {
        this.ensuerTime = ensuerTime;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

}
