package com.mistra.plank.model.vo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageVo<T> {

    private int totalRecords;
    private List<T> data;

    private Map<String, Object> extraData;

    public PageVo(List<T> data, int totalRecords) {
        this.data = data;
        this.totalRecords = totalRecords;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public Map<String, Object> getExtraData() {
        return extraData;
    }

    public void putExtraData(String key, Object value) {
        if (extraData == null) {
            extraData = new HashMap<>();
        }
        extraData.put(key, value);
    }

}
