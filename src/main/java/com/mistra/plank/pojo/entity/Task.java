package com.mistra.plank.pojo.entity;

import com.mistra.plank.exception.ServiceException;

public enum Task {

    BeginOfYear(1, "begin_of_year"), BeginOfDay(2, "begin_of_day"),
    UpdateOfStock(3, "update_of_stock"), UpdateOfDailyIndex(4, "update_of_daily_index"),
    Ticker(5, "ticker"), TradeTicker(6, "trade_ticker"),
    ApplyNewStock(7, "apply_new_stock"), AutoLogin(8, "auto_login");

    private int id;
    private String name;

    Task(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static Task valueOf(int id) {
        for (Task task : Task.values()) {
            if (task.id == id) {
                return task;
            }
        }
        throw new ServiceException("no such id of Task");
    }

}
