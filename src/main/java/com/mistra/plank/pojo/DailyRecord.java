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
import java.util.Date;

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
@TableName(value = "daily_record", autoResultMap = true)
public class DailyRecord {

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
     * 昨日收盘价
     */
    @TableField(value = "yesterday_close_price")
    private BigDecimal yesterdayClosePrice;

    /**
     * 开盘价
     */
    @TableField(value = "open_price")
    private BigDecimal openPrice;

    /**
     * 收盘价
     */
    @TableField(value = "close_price")
    private BigDecimal closePrice;

    /**
     * 涨跌比率
     */
    @TableField(value = "increase_rate")
    private BigDecimal increaseRate;

    /**
     * 日期
     */
    @TableField(value = "date")
    private Date date;
}
