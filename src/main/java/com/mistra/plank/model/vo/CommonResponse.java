package com.mistra.plank.model.vo;

public class CommonResponse {

    public static final String DEFAULT_MESSAGE_SUCCESS = "success";

    private String message;

    public CommonResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static CommonResponse buildResponse(String message) {
        return new CommonResponse(message);
    }

    @Override
    public String toString() {
        return "CommonResponse [message=" + message + "]";
    }

}
