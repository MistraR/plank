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
@TableName(value = "robot", autoResultMap = true)
public class Robot extends BaseModel {

    private static final long serialVersionUID = 1L;

    @TableField(value = "type")
    private Integer type;
    @TableField(value = "webhook")
    private String webhook;
    @TableField(value = "state")
    private Integer state;

}
