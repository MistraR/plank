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
@TableName(value = "system_config", autoResultMap = true)
public class SystemConfig extends BaseModel {

    private static final long serialVersionUID = 1L;
    @TableField(value = "name")
    private String name;
    @TableField(value = "value1")
    private String value1;
    @TableField(value = "value2")
    private String value2;
    @TableField(value = "value3")
    private String value3;
    @TableField(value = "state")
    private Integer state;

}
