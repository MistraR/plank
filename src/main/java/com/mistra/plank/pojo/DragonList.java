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
    private Integer netBuy;

    /**
     * 买入前5合计(W)
     */
    @TableField(value = "first_five_net_buy")
    private Integer firstFiveNetBuy;

    /**
     * 卖出前5合计(W)
     */
    @TableField(value = "first_five_net_sell")
    private Integer firstFiveNetSell;

    /**
     * 最近5日涨跌幅
     */
    @TableField(value = "last_five_days_rate")
    private BigDecimal lastFiveDaysRate;

    /**
     * 日期
     */
    @TableField(value = "date")
    private Date date;

}
