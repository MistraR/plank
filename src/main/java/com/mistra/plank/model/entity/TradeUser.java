package com.mistra.plank.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "trade_user", autoResultMap = true)
public class TradeUser extends BaseModel {

    private static final long serialVersionUID = 1L;

    @TableField(value = "account_id")
    private String accountId;
    @TableField(value = "password")
    private String password;
    @TableField(value = "validate_key")
    private String validateKey;
    @TableField(value = "cookie")
    private String cookie;

}
