package com.mistra.plank.service;

import java.util.List;

import com.mistra.plank.model.entity.DailyIndex;
import com.mistra.plank.model.entity.StockInfo;

public interface StockCrawlerService {

    List<StockInfo> getStockList();

    DailyIndex getDailyIndex(String code);

    List<DailyIndex> getDailyIndex(List<String> codeList);

    List<DailyIndex> getDailyIndexFromEastMoney();

    List<DailyIndex> getHistoryDailyIndexs(String code);

    String getHistoryDailyIndexsString(String code);

    String getHistoryDailyIndexsStringFrom163(String code, int year, int season);

}
