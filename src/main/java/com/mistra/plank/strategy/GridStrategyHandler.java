package com.mistra.plank.strategy;

import com.mistra.plank.common.exception.ServiceException;
import com.mistra.plank.common.util.DecimalUtil;
import com.mistra.plank.common.util.StockConsts;
import com.mistra.plank.common.util.StockUtil;
import com.mistra.plank.common.util.TradeUtil;
import com.mistra.plank.model.entity.TradeOrder;
import com.mistra.plank.model.vo.trade.TradeRuleVo;
import com.mistra.plank.service.MessageService;
import com.mistra.plank.service.StockInfoService;
import com.mistra.plank.service.TradeApiService;
import com.mistra.plank.service.TradeService;
import com.mistra.plank.strategy.model.GridStrategyInput;
import com.mistra.plank.strategy.model.GridStrategyResult;
import com.mistra.plank.strategy.model.StrategySubmitResult;
import com.mistra.plank.tradeapi.TradeResultVo;
import com.mistra.plank.tradeapi.request.GetDealDataRequest;
import com.mistra.plank.tradeapi.request.GetOrdersDataRequest;
import com.mistra.plank.tradeapi.request.RevokeRequest;
import com.mistra.plank.tradeapi.request.SubmitRequest;
import com.mistra.plank.tradeapi.response.GetDealDataResponse;
import com.mistra.plank.tradeapi.response.GetOrdersDataResponse;
import com.mistra.plank.tradeapi.response.RevokeResponse;
import com.mistra.plank.tradeapi.response.SubmitResponse;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@Component("gridStrategyHandler")
public class GridStrategyHandler extends BaseStrategyHandler<GridStrategyInput, GridStrategyResult> {

    private static final Logger logger = LoggerFactory.getLogger(GridStrategyHandler.class);

    @Autowired
    private MessageService messageServicve;

    @Autowired
    protected TradeApiService tradeApiService;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private StockInfoService stockService;

    @Override
    public GridStrategyInput queryInput(TradeRuleVo tradeRuleVo) {
        TradeResultVo<GetDealDataResponse> dealData = getDealData(tradeRuleVo.getUserId());
        if (!dealData.success()) {
            throw new ServiceException("execute GridStrategyHandler get deal error: " + dealData.getMessage());
        }

        TradeResultVo<GetOrdersDataResponse> orderData = getOrderData(tradeRuleVo.getUserId());
        if (!orderData.success()) {
            throw new ServiceException("execute GridStrategyHandler get order error: " + dealData.getMessage());
        }

        List<GetDealDataResponse> dealDataList = TradeUtil.mergeDealList(dealData.getData()
                .stream().filter(v -> v.getZqdm().equals(tradeRuleVo.getStockCode())).collect(Collectors.toList()));

        // 30 day, yibao yicheng
        List<TradeOrder> tradeOrderList = tradeService.getLastTradeOrderListByRuleId(tradeRuleVo.getId().intValue(), tradeRuleVo.getUserId());
        tradeOrderList = tradeOrderList.stream().filter(v -> v.getStockCode().equals(tradeRuleVo.getStockCode())).collect(Collectors.toList());

        updateTradeState(tradeOrderList, dealDataList, orderData.getData());

        GridStrategyInput input = new GridStrategyInput(tradeRuleVo);
        input.setDealDataList(dealDataList);
        input.setTradeOrderList(tradeOrderList);

        return input;
    }

    private void updateTradeState(List<TradeOrder> tradeOrderList, List<GetDealDataResponse> dealDataList, List<GetOrdersDataResponse> orderDataList) {
        long last5MinTime = DateUtils.addMinutes(new Date(), -1).getTime();
        for (TradeOrder tradeOrder : tradeOrderList) {
            GetDealDataResponse dealData = getByCondition(dealDataList, v -> tradeOrder.getEntrustCode().equals(v.getWtbh()));
            if (dealData != null) {
                tradeOrder.setDealCode(dealData.getCjbh());
                tradeOrder.setEntrustCode(dealData.getWtbh());
                tradeOrder.setPrice(new BigDecimal(dealData.getCjjg()));
                tradeOrder.setTradeState(GetOrdersDataResponse.YICHENG);

                Date tradeTime = tradeOrder.getTradeTime();
                tradeTime = DateUtils.setHours(tradeTime, Integer.parseInt(dealData.getCjsj().substring(0, 2)));
                tradeTime = DateUtils.setMinutes(tradeTime, Integer.parseInt(dealData.getCjsj().substring(2, 4)));
                tradeTime = DateUtils.setSeconds(tradeTime, Integer.parseInt(dealData.getCjsj().substring(4, 6)));

                tradeOrder.setTradeTime(tradeTime);
                tradeOrder.setTradeType(dealData.getMmlb());
                tradeOrder.setVolume(Integer.parseInt(dealData.getCjsl()));
            }

            if (!tradeOrder.isDealed()) {
                if (!DateUtils.isSameDay(tradeOrder.getTradeTime(), new Date())) {
                    tradeOrder.setTradeState(GetOrdersDataResponse.YICHE);
                } else {
                    GetOrdersDataResponse orderData = getByCondition(orderDataList, v -> tradeOrder.getEntrustCode().equals(v.getWtbh()));
                    if (orderData == null) {
                        if (tradeOrder.getTradeTime().getTime() <= last5MinTime) {
                            tradeOrder.setTradeState(GetOrdersDataResponse.YICHE);
                        }
                    } else {
                        if (GetOrdersDataResponse.YICHE.equals(orderData.getWtzt())) {
                            tradeOrder.setTradeState(GetOrdersDataResponse.YICHE);
                        }
                    }
                }
            }
        }
    }

    @Override
    public GridStrategyResult handle(GridStrategyInput input) {
        TradeRuleVo tradeRuleVo = input.getTradeRuleVo();
        List<TradeOrder> tradeOrderList = input.getTradeOrderList();
        tradeOrderList = tradeOrderList.stream().filter(v -> v.isValid()).collect(Collectors.toList());

        ArrayList<String> revokeList = new ArrayList<>();
        ArrayList<StrategySubmitResult> submitList = new ArrayList<>();

        boolean isHandle = false;

        for (TradeOrder tradeOrder : tradeOrderList) {
            if (tradeOrder.isDealed()) {
                TradeOrder relatedTradeOrder = getByCondition(tradeOrderList, v -> tradeOrder.getRelatedDealCode().equals(v.getRelatedDealCode()) && !tradeOrder.getTradeType().equals(v.getTradeType()));
                if (relatedTradeOrder != null && GetOrdersDataResponse.YIBAO.equals(relatedTradeOrder.getTradeState())) {
                    revokeList.add(relatedTradeOrder.getEntrustCode());
                }

                TradeOrder buyTradeOrder = getByCondition(tradeOrderList, v -> SubmitRequest.B.equals(v.getTradeType()) && tradeOrder.getDealCode().equals(v.getRelatedDealCode()));
                if (buyTradeOrder == null) {
                    setNeedSubmit(tradeOrder.getPrice().doubleValue(), tradeOrder.getDealCode(), submitList, tradeRuleVo, SubmitRequest.B);
                }

                TradeOrder sellTradeOrder = getByCondition(tradeOrderList, v -> SubmitRequest.S.equals(v.getTradeType()) && tradeOrder.getDealCode().equals(v.getRelatedDealCode()));
                if (sellTradeOrder == null) {
                    setNeedSubmit(tradeOrder.getPrice().doubleValue(), tradeOrder.getDealCode(), submitList, tradeRuleVo, SubmitRequest.S);
                }
                isHandle = true;
                break;
            }
        }

        if (!isHandle) {
            String dealCode = "m" + System.currentTimeMillis();
            if (tradeOrderList.stream().noneMatch(v -> GetOrdersDataResponse.YIBAO.equals(v.getTradeState()) && v.isManual() && SubmitRequest.B.equals(v.getTradeType()))) {
                setNeedSubmit(tradeRuleVo.getOpenPrice().doubleValue(), dealCode, submitList, tradeRuleVo, SubmitRequest.B);
            }
            if (tradeOrderList.stream().noneMatch(v -> GetOrdersDataResponse.YIBAO.equals(v.getTradeState()) && v.isManual() && SubmitRequest.S.equals(v.getTradeType()))) {
                setNeedSubmit(tradeRuleVo.getOpenPrice().doubleValue(), dealCode, submitList, tradeRuleVo, SubmitRequest.S);
            }
        }

        GridStrategyResult result = new GridStrategyResult();
        result.setRevokeList(revokeList);
        result.setSubmitList(submitList);
        return result;
    }

    private void setNeedSubmit(double price, String dealCode, List<StrategySubmitResult> submitList, TradeRuleVo tradeRuleVo, String tradeType) {
        String stockCode = tradeRuleVo.getStockCode();
        int amount = tradeRuleVo.getVolume();
        double value = tradeRuleVo.getValue().doubleValue();

        double tradePrice;
        if (SubmitRequest.B.equals(tradeType)) {
            value = -value;
        }

        if (tradeRuleVo.isProportion()) {
            tradePrice = (int) (price * (1 + value) * getPrecision(stockCode)) / getPrecision(stockCode);
        } else {
            tradePrice = price + value;
        }

        StrategySubmitResult result = new StrategySubmitResult(tradeRuleVo.getUserId());
        result.setAmount(amount);
        result.setPrice(tradePrice);
        result.setTradeType(tradeType);
        result.setStockCode(stockCode);
        result.setRelatedDealCode(dealCode);
        result.setZqmc(tradeRuleVo.getStockName());
        result.setMarket(StockUtil.getStockMarket(stockCode));
        submitList.add(result);
    }

    @Override
    public void handleResult(GridStrategyInput input, GridStrategyResult result) {
        List<String> revokeList = result.getRevokeList();
        List<StrategySubmitResult> submitList = result.getSubmitList();

        revokeList.forEach(entrustCode -> {
            String revokes = String.format("%s_%s", DateFormatUtils.format(new Date(), "yyyyMMdd"), entrustCode);
            logger.info("revoke request: {}", revokes);
            TradeResultVo<RevokeResponse> resultVo = revoke(input.getUserId(), revokes);
            logger.info("revoke response: {}", resultVo);
            if (resultVo.success()) {
                input.getTradeOrderList().forEach(v -> {
                    if (v.getEntrustCode().equals(entrustCode)) {
                        v.setTradeState(GetOrdersDataResponse.YICHE);
                    }
                });
            } else {
                logger.error(resultVo.getMessage());
                messageServicve.send(String.format("revoke error. request: %s, response: %s", revokes, resultVo.getMessage()));
            }
        });

        ArrayList<TradeOrder> tradeOrderList = new ArrayList<>();

        BigDecimal lowestPrice = input.getTradeRuleVo().getLowestPrice();
        BigDecimal highestPrice = input.getTradeRuleVo().getHighestPrice();

        submitList.forEach(request -> {
            BigDecimal bPrice = BigDecimal.valueOf(request.getPrice());
            if (DecimalUtil.ls(bPrice, lowestPrice)) {
                messageServicve.send(String.format("code %s, lowestPrice %.03f, price %.03f", request.getStockCode(), lowestPrice.doubleValue(), request.getPrice()));
                return;
            }
            if (DecimalUtil.bg(bPrice, highestPrice)) {
                messageServicve.send(String.format("code %s, highestPrice %.03f, price %.03f", request.getStockCode(), highestPrice.doubleValue(), request.getPrice()));
                return;
            }

            TradeResultVo<SubmitResponse> saleResultVo = trade(request);
            if (saleResultVo.success()) {
                TradeOrder tradeOrder = new TradeOrder();
                tradeOrder.setRuleId(input.getTradeRuleVo().getId().intValue());
                tradeOrder.setDealCode("");
                tradeOrder.setEntrustCode(saleResultVo.getData().get(0).getWtbh());
                tradeOrder.setRelatedDealCode(request.getRelatedDealCode());
                tradeOrder.setStockCode(request.getStockCode());
                tradeOrder.setPrice(bPrice);
                tradeOrder.setVolume(request.getAmount());
                tradeOrder.setTradeType(request.getTradeType());
                tradeOrder.setTradeState(GetOrdersDataResponse.YIBAO);
                tradeOrder.setTradeTime(new Date());
                tradeOrder.setState(StockConsts.TradeState.Valid.value());
                tradeOrderList.add(tradeOrder);
            }
        });
        tradeOrderList.addAll(input.getTradeOrderList());

        tradeService.saveTradeOrderList(tradeOrderList);
    }

    private TradeResultVo<SubmitResponse> trade(SubmitRequest request) {
        logger.info("submit request: {}", request);
        TradeResultVo<SubmitResponse> tradeResultVo = submit(request);
        logger.info("submit response: {}", tradeResultVo);
        String name = stockService.getStockByFullCode(StockUtil.getFullCode(request.getStockCode())).getName();
        if (!tradeResultVo.success()) {
            logger.error(tradeResultVo.getMessage());
        }
        String body = String.format("%s submit %s %s %d %.03f %s", getFlag(), request.getTradeType(), name, request.getAmount(), request.getPrice(), tradeResultVo.getMessage() == null ? "" : tradeResultVo.getMessage());
        try {
            messageServicve.send(body);
        } catch (Exception e) {
            logger.error("send message error", e);
        }
        return tradeResultVo;
    }

    protected TradeResultVo<GetOrdersDataResponse> getOrderData(int userId) {
        return tradeApiService.getOrdersData(new GetOrdersDataRequest(userId));
    }

    protected TradeResultVo<GetDealDataResponse> getDealData(int userId) {
        return tradeApiService.getDealData(new GetDealDataRequest(userId));
    }

    protected TradeResultVo<SubmitResponse> submit(SubmitRequest request) {
        return tradeApiService.submit(request);
    }

    protected TradeResultVo<RevokeResponse> revoke(int userId, String revokes) {
        RevokeRequest request = new RevokeRequest(userId);
        request.setRevokes(revokes);
        TradeResultVo<RevokeResponse> resultVo = tradeApiService.revoke(request);
        return resultVo;
    }

    protected String getFlag() {
        return "normal";
    }

    private <T> T getByCondition(List<T> list, Predicate<T> predicate) {
        Optional<T> optional = list.stream().filter(predicate).findAny();
        return optional.orElse(null);
    }

    private double getPrecision(String code) {
        int type = StockUtil.getStockType(null, code);
        if (type == StockConsts.StockType.ETF.value()) {
            return 1000;
        }
        return 100;
    }

}
