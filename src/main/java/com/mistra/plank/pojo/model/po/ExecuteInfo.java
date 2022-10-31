package com.mistra.plank.pojo.model.po;

import java.util.Date;

public class ExecuteInfo extends BaseModel {

    private static final long serialVersionUID = 1L;

    private int taskId;
    private Date startTime;
    private Date completeTime;
    private String paramsStr;
    private boolean isManual;
    private int state;
    private String message;

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public String getParamsStr() {
        return paramsStr;
    }

    public void setParamsStr(String paramsStr) {
        this.paramsStr = paramsStr;
    }

    public Date getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(Date completeTime) {
        this.completeTime = completeTime;
    }

    public boolean isManual() {
        return isManual;
    }

    public void setManual(boolean isManual) {
        this.isManual = isManual;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ExecuteInfo [taskId=" + taskId + ", startTime=" + startTime + ", completeTime=" + completeTime
                + ", paramsStr=" + paramsStr + ", isManual=" + isManual + ", state=" + state + ", message=" + message
                + "]";
    }

}
