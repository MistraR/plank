package com.mistra.plank.service;

import com.mistra.plank.pojo.entity.DailyIndex;
import com.mistra.plank.pojo.entity.StockInfo;
import com.mistra.plank.pojo.entity.StockLog;
import com.mistra.plank.pojo.vo.DailyIndexVo;
import com.mistra.plank.pojo.vo.PageParam;
import com.mistra.plank.pojo.vo.PageVo;

import java.util.Date;
import java.util.List;

public interface StockInfoService {

    List<StockInfo> getAll();

    List<StockInfo> getAllListed();

    void addStockLog(List<StockLog> list);

    void update(List<StockInfo> needAddedList, List<StockInfo> needUpdatedList, List<StockLog> stockLogList);

    void saveDailyIndexToFile(String rootPath);

    void saveDailyIndexFromFile(String rootPath);

    void saveDailyIndex(List<DailyIndex> list);

    PageVo<StockInfo> getStockList(PageParam pageParam);

    StockInfo getStockByFullCode(String code);

    PageVo<DailyIndexVo> getDailyIndexList(PageParam pageParam);

    List<DailyIndex> getDailyIndexListByDate(Date date);

    void fixDailyIndex(int date, List<String> codeList);

}
