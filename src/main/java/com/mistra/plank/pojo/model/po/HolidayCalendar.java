package com.mistra.plank.pojo.model.po;

import java.util.Date;

public class HolidayCalendar extends BaseModel {

    private static final long serialVersionUID = 1L;

    private Date date;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "HolidayCalendar [date=" + date + ", toString()=" + super.toString() + "]";
    }

}
