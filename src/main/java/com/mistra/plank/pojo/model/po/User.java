package com.mistra.plank.pojo.model.po;

import com.alibaba.fastjson.annotation.JSONField;

public class User extends BaseModel {

    private static final long serialVersionUID = 1L;

    private String username;
    @JSONField(serialize=false)
    private String password;
    private String name;
    private String mobile;
    private String email;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "User [username=" + username + ", password=" + password + ", name=" + name
                + ", mobile=" + mobile + ", email=" + email + ", " + super.toString() + "]";
    }

}
