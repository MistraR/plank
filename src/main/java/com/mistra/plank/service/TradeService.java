package com.mistra.plank.service;

import java.util.Date;
import java.util.List;


import com.mistra.plank.api.response.CrQueryCollateralResponse;
import com.mistra.plank.api.response.GetDealDataResponse;
import com.mistra.plank.api.response.GetOrdersDataResponse;
import com.mistra.plank.api.response.GetStockListResponse;
import com.mistra.plank.pojo.model.po.StockSelected;
import com.mistra.plank.pojo.model.po.TradeDeal;
import com.mistra.plank.pojo.model.po.TradeMethod;
import com.mistra.plank.pojo.model.po.TradeOrder;
import com.mistra.plank.pojo.model.po.TradeUser;
import com.mistra.plank.pojo.model.vo.PageParam;
import com.mistra.plank.pojo.model.vo.PageVo;
import com.mistra.plank.pojo.model.vo.trade.DealVo;
import com.mistra.plank.pojo.model.vo.trade.OrderVo;
import com.mistra.plank.pojo.model.vo.trade.StockVo;
import com.mistra.plank.pojo.model.vo.trade.TradeRuleVo;


public interface TradeService {

    TradeMethod getTradeMethodByName(String name);

    List<TradeUser> getTradeUserList();

    TradeUser getTradeUserById(int id);

    void updateTradeUser(TradeUser tradeUser);

    List<DealVo> getTradeDealList(List<? extends GetDealDataResponse> data);

    List<StockVo> getTradeStockList(List<GetStockListResponse> stockList);

    List<StockVo> getCrTradeStockList(List<CrQueryCollateralResponse> stockList);

    List<OrderVo> getTradeOrderList(List<? extends GetOrdersDataResponse> orderList);

    List<StockVo> getTradeStockListBySelected(List<StockSelected> selectList);

    PageVo<TradeRuleVo> getTradeRuleList(PageParam pageParam);

    void changeTradeRuleState(int state, int id);

    List<TradeOrder> getLastTradeOrderListByRuleId(int ruleId, int userId);

    void saveTradeOrderList(List<TradeOrder> tradeOrderList);

    void resetRule(int id);

    List<TradeDeal> getTradeDealListByDate(Date date);

    void saveTradeDealList(List<TradeDeal> list);

}
