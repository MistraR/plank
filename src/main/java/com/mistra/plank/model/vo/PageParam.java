package com.mistra.plank.model.vo;

import java.util.HashMap;
import java.util.Map;

public class PageParam {

    private int start;
    private int length;
    private Integer tradeUserId;
    private Map<String, Object> condition = new HashMap<>();

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public Integer getTradeUserId() {
        return tradeUserId;
    }

    public void setTradeUserId(Integer tradeUserId) {
        this.tradeUserId = tradeUserId;
    }

    public Map<String, Object> getCondition() {
        return condition;
    }

    public void putCondition(Map<String, ?> map) {
        condition.putAll(map);
    }

    public void putCondition(String key, Object value) {
        condition.put(key, value);
    }

}
