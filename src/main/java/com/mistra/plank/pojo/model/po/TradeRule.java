package com.mistra.plank.pojo.model.po;

import com.mistra.plank.util.StockConsts;

import java.math.BigDecimal;

public class TradeRule extends BaseModel {

    private static final long serialVersionUID = 1L;

    private String stockCode;
    private int strategyId;
    private int userId;
    private int type;
    private BigDecimal value;
    private int volume;
    private BigDecimal openPrice;
    private BigDecimal highestPrice;
    private BigDecimal lowestPrice;
    private BigDecimal highestVolume;
    private BigDecimal lowestVolume;
    private int state;
    private String description;

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public int getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(int strategyId) {
        this.strategyId = strategyId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
    }

    public BigDecimal getHighestPrice() {
        return highestPrice;
    }

    public void setHighestPrice(BigDecimal highestPrice) {
        this.highestPrice = highestPrice;
    }

    public BigDecimal getLowestPrice() {
        return lowestPrice;
    }

    public void setLowestPrice(BigDecimal lowestPrice) {
        this.lowestPrice = lowestPrice;
    }

    public BigDecimal getHighestVolume() {
        return highestVolume;
    }

    public void setHighestVolume(BigDecimal highestVolume) {
        this.highestVolume = highestVolume;
    }

    public BigDecimal getLowestVolume() {
        return lowestVolume;
    }

    public void setLowestVolume(BigDecimal lowestVolume) {
        this.lowestVolume = lowestVolume;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isValid() {
        return state == StockConsts.TradeState.Valid.value();
    }

    public boolean isProportion() {
        return CalcType.PROPORTION.value == type;
    }

    public enum CalcType {
        PROPORTION(0), DIFFERENCE(1);
        private int value;

        private CalcType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

}
