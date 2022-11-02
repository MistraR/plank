package com.mistra.plank.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mistra.plank.common.util.StockConsts;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "stock_info", autoResultMap = true)
public class StockInfo extends BaseModel {

    private static final long serialVersionUID = 1L;
    @TableField(value = "code")
    private String code;
    @TableField(value = "name")
    private String name;
    @TableField(value = "exchange")
    private String exchange;
    @TableField(value = "abbreviation")
    private String abbreviation;
    @TableField(value = "state")
    private Integer state;
    @TableField(value = "type")
    private Integer type;

    public boolean isValid() {
        return state != StockConsts.StockState.Terminated.value();
    }

    public boolean isA() {
        return type == StockConsts.StockType.A.value();
    }

    public boolean isIndex() {
        return type == StockConsts.StockType.Index.value();
    }

    public String getFullCode() {
        return exchange + code;
    }

}
