package com.mistra.plank.pojo.model.po;

import java.math.BigDecimal;

public class StockSelected extends BaseModel {

    private static final long serialVersionUID = 1L;

    private String code;
    private BigDecimal rate;
    private String description;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
