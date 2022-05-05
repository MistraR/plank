package com.mistra.plank.pojo;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@TableName(value = "dragon_list", autoResultMap = true)
public class DragonList {

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
     * 净买入(W)
     */
    @TableField(value = "net_buy")
    private Long netBuy;

    /**
     * 买入合计(W)
     */
    @TableField(value = "buy")
    private Long buy;

    /**
     * 卖出合计(W)
     */
    @TableField(value = "sell")
    private Long sell;

    /**
     * 收盘价
     */
    @TableField(value = "price")
    private BigDecimal price;

    /**
     * 流通市值
     */
    @TableField(value = "market_value")
    private Long marketValue;

    /**
     * 成交额
     */
    @TableField(value = "accum_amount")
    private Long accumAmount;

    /**
     * 涨跌幅
     */
    @TableField(value = "change_rate")
    private BigDecimal changeRate;

    /**
     * 日期
     */
    @TableField(value = "date")
    private Date date;

}
