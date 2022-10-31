package com.mistra.plank.pojo.model.po;

import java.util.Date;

public class StockLog extends BaseModel {

    private static final long serialVersionUID = 1L;

    private int stockInfoId;
    private Date date;
    private int type;
    private String oldValue;
    private String newValue;

    public StockLog() {
    }

    public StockLog(int stockInfoId, Date date, int type, String oldValue, String newValue) {
        super();
        this.stockInfoId = stockInfoId;
        this.date = date;
        this.type = type;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public int getStockInfoId() {
        return stockInfoId;
    }

    public void setStockInfoId(int stockInfoId) {
        this.stockInfoId = stockInfoId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    @Override
    public String toString() {
        return "StockLog [stockInfoId=" + stockInfoId + ", date=" + date + ", type=" + type + ", oldValue=" + oldValue
                + ", newValue=" + newValue + ", toString()=" + super.toString() + "]";
    }

}
