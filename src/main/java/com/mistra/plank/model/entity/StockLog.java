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
@TableName(value = "stock_log", autoResultMap = true)
public class StockLog extends BaseModel {

    private static final long serialVersionUID = 1L;

    @TableField(value = "stock_info_id")
    private Integer stockInfoId;
    @TableField(value = "date")
    private Date date;
    @TableField(value = "type")
    private Integer type;
    @TableField(value = "old_value")
    private String oldValue;
    @TableField(value = "new_value")
    private String newValue;
}
