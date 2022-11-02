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
@TableName(value = "holiday_calendar", autoResultMap = true)
public class HolidayCalendar extends BaseModel {

    private static final long serialVersionUID = 1L;

    @TableField(value = "date")
    private Date date;
}
