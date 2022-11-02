package com.mistra.plank.service;

import java.util.Date;
import java.util.List;


import com.mistra.plank.tradeapi.response.CrQueryCollateralResponse;
import com.mistra.plank.tradeapi.response.GetDealDataResponse;
import com.mistra.plank.tradeapi.response.GetOrdersDataResponse;
import com.mistra.plank.tradeapi.response.GetStockListResponse;
import com.mistra.plank.model.entity.StockSelected;
import com.mistra.plank.model.entity.TradeDeal;
import com.mistra.plank.model.entity.TradeMethod;
import com.mistra.plank.model.entity.TradeOrder;
import com.mistra.plank.model.entity.TradeUser;
import com.mistra.plank.model.vo.PageParam;
import com.mistra.plank.model.vo.PageVo;
import com.mistra.plank.model.vo.trade.DealVo;
import com.mistra.plank.model.vo.trade.OrderVo;
import com.mistra.plank.model.vo.trade.StockVo;
import com.mistra.plank.model.vo.trade.TradeRuleVo;


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
