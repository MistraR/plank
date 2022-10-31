package com.mistra.plank.pojo.model.po;

public class TradeMethod extends BaseModel {

    private static final long serialVersionUID = 1L;

    private String name;
    private String url;
    private int state;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

}
