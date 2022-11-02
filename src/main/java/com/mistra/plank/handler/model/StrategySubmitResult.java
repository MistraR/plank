package com.mistra.plank.handler.model;

import com.mistra.plank.api.request.SubmitRequest;

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
