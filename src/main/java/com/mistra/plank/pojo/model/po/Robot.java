package com.mistra.plank.pojo.model.po;

public class Robot extends BaseModel {

    private static final long serialVersionUID = 1L;

    private int type;
    private String webhook;
    private int state;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getWebhook() {
        return webhook;
    }

    public void setWebhook(String webhook) {
        this.webhook = webhook;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

}
