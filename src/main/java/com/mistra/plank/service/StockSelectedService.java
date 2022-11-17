package com.mistra.plank.service;


import com.mistra.plank.model.entity.StockSelected;
import com.mistra.plank.model.param.AutoTradeParam;
import com.mistra.plank.model.param.SelfSelectParam;

import java.util.List;

public interface StockSelectedService {

    List<StockSelected> getList();

    void addSelfSelect(SelfSelectParam selfSelectParam);

    void tomorrowAutoTradePool(List<AutoTradeParam> autoTradeParams);
}
