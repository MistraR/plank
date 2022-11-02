package com.mistra.plank.strategy.model;

import com.mistra.plank.tradeapi.request.SubmitRequest;

public class StrategySubmitResult extends SubmitRequest {

    private String relatedDealCode;

    public StrategySubmitResult(int userId) {
        super(userId);
    }

    public String getRelatedDealCode() {
        return relatedDealCode;
    }

    public void setRelatedDealCode(String relatedDealCode) {
        this.relatedDealCode = relatedDealCode;
    }



}
