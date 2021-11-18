package com.mistra.plank.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2021/11/18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "hold_shares", autoResultMap = true)
public class HoldShares {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField
    private String name;

    /**
     * 证券代码
     */
    @TableField
    private String code;

    /**
     * 持股数量
     */
    @TableField(value = "number")
    private Integer number;

    /**
     * 成本价
     */
    @TableField(value = "cost")
    private BigDecimal cost;

    /**
     * 当前价
     */
    @TableField(value = "current_price")
    private BigDecimal currentPrice;

    /**
     * 盈亏比率
     */
    @TableField(value = "rate")
    private BigDecimal rate;
}
