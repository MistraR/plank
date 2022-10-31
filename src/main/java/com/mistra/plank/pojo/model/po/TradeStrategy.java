package com.mistra.plank.pojo.model.po;

public class TradeStrategy extends BaseModel {

    private static final long serialVersionUID = 1L;

    private String name;
    private String beanName;
    private int state;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

}
