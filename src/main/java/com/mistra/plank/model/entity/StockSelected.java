package com.mistra.plank.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "stock_selected", autoResultMap = true)
public class StockSelected extends BaseModel {

    private static final long serialVersionUID = 1L;

    @TableField(value = "code")
    private String code;
    @TableField(value = "rate")
    private BigDecimal rate;
    @TableField(value = "description")
    private String description;

}
