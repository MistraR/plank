package com.mistra.plank.strategy.model;

import java.util.List;

public class GridStrategyResult {

    private List<String> revokeList;
    private List<StrategySubmitResult> submitList;

    public List<String> getRevokeList() {
        return revokeList;
    }

    public void setRevokeList(List<String> revokeList) {
        this.revokeList = revokeList;
    }

    public List<StrategySubmitResult> getSubmitList() {
        return submitList;
    }

    public void setSubmitList(List<StrategySubmitResult> submitList) {
        this.submitList = submitList;
    }

}
