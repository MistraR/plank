package com.mistra.plank.pojo.model.po;

import com.mistra.plank.api.response.GetOrdersDataResponse;

import java.math.BigDecimal;
import java.util.Date;

public class TradeOrder extends BaseModel {

    private static final long serialVersionUID = 1L;

    private int ruleId;
    private String stockCode;
    private String entrustCode;
    private String dealCode;
    private String relatedDealCode;
    private BigDecimal price;
    private int volume;
    private String tradeType;
    private String tradeState;
    private Date tradeTime;
    private int state;

    public int getRuleId() {
        return ruleId;
    }

    public void setRuleId(int ruleId) {
        this.ruleId = ruleId;
    }

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getEntrustCode() {
        return entrustCode;
    }

    public void setEntrustCode(String entrustCode) {
        this.entrustCode = entrustCode;
    }

    public String getDealCode() {
        return dealCode;
    }

    public void setDealCode(String dealCode) {
        this.dealCode = dealCode;
    }

    public String getRelatedDealCode() {
        return relatedDealCode;
    }

    public void setRelatedDealCode(String relatedDealCode) {
        this.relatedDealCode = relatedDealCode;
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

    public String getTradeState() {
        return tradeState;
    }

    public void setTradeState(String tradeState) {
        this.tradeState = tradeState;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Date getTradeTime() {
        return tradeTime;
    }

    public void setTradeTime(Date tradeTime) {
        this.tradeTime = tradeTime;
    }

    public boolean isDealed() {
        return GetOrdersDataResponse.YICHENG.equals(tradeState);
    }

    public boolean isValid() {
        return isDealed() || GetOrdersDataResponse.WEIBAO.equals(tradeState) || GetOrdersDataResponse.YIBAO.equals(tradeState);
    }

    public boolean isManual() {
        return relatedDealCode.startsWith("m");
    }

}
