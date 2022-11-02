package com.mistra.plank.tradeapi;

import java.util.List;

public class TradeResultVo<T> {

    public static final int STATUS_SUCCESS = 0;

    private String Message;
    private int Status;
    private int Count;
    private int Errcode;
    private List<T> Data;

    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        Message = message;
    }

    public int getStatus() {
        return Status;
    }

    public void setStatus(int status) {
        Status = status;
    }

    public int getCount() {
        return Count;
    }

    public void setCount(int count) {
        Count = count;
    }

    public int getErrcode() {
        return Errcode;
    }

    public void setErrcode(int errcode) {
        Errcode = errcode;
    }

    public List<T> getData() {
        return Data;
    }

    public void setData(List<T> data) {
        Data = data;
    }

    public boolean success() {
        return TradeResultVo.success(Status);
    }

    public static boolean success(int Status) {
        return Status == TradeResultVo.STATUS_SUCCESS;
    }

}
