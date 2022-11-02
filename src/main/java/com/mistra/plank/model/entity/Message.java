package com.mistra.plank.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "trade_deal", autoResultMap = true)
public class Message extends BaseModel {

    private static final long serialVersionUID = 1L;

    @TableField(value = "value")
    private Integer type;
    @TableField(value = "target")
    private String target;
    @TableField(value = "body")
    private String body;
    @TableField(value = "send_time")
    private Date sendTime;

}
