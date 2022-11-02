package com.mistra.plank.service;

import java.util.Date;

public interface HolidayCalendarService {

    void updateCurrentYear();

    boolean isBusinessDate(Date date);

    boolean isBusinessTime(Date date);

}
