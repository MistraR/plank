package com.mistra.plank.controller;

import com.mistra.plank.model.entity.StockInfo;
import com.mistra.plank.model.vo.DailyIndexVo;
import com.mistra.plank.model.vo.PageParam;
import com.mistra.plank.model.vo.PageVo;
import com.mistra.plank.service.StockInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("report")
public class ReportController extends BaseController {

    @Autowired
    private StockInfoService stockService;

    @RequestMapping("stockList")
    public PageVo<StockInfo> getStockList(PageParam pageParam) {
        return stockService.getStockList(pageParam);
    }

    @RequestMapping("dailyIndexList")
    public PageVo<DailyIndexVo> getDailyIndexList(PageParam pageParam) {
        return stockService.getDailyIndexList(pageParam);
    }

}
