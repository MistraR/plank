package com.mistra.plank.pojo.model.po;

import java.util.Date;

public class Message extends BaseModel {

    private static final long serialVersionUID = 1L;

    private int type;
    private String target;
    private String body;
    private Date sendTime;

    public Message() {
    }

    public Message(int type, String target, String body, Date sendTime) {
        this.type = type;
        this.target = target;
        this.body = body;
        this.sendTime = sendTime;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }

    @Override
    public String toString() {
        return "Message [type=" + type + ", target=" + target + ", body=" + body + ", sendTime=" + sendTime
                + ", toString()=" + super.toString() + "]";
    }

}
