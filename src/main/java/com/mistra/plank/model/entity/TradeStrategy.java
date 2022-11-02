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
@TableName(value = "trade_strategy", autoResultMap = true)
public class TradeStrategy extends BaseModel {

    private static final long serialVersionUID = 1L;

    @TableField(value = "name")
    private String name;
    @TableField(value = "bean_name")
    private String beanName;
    @TableField(value = "state")
    private Integer state;
}
