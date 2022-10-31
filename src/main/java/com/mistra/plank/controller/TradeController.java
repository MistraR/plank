package com.mistra.plank.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.mistra.plank.api.TradeResultVo;
import com.mistra.plank.api.request.AuthenticationRequest;
import com.mistra.plank.api.request.BaseTradeRequest;
import com.mistra.plank.api.request.GetAssetsRequest;
import com.mistra.plank.api.request.GetDealDataRequest;
import com.mistra.plank.api.request.GetHisDealDataRequest;
import com.mistra.plank.api.request.GetOrdersDataRequest;
import com.mistra.plank.api.request.GetStockListRequest;
import com.mistra.plank.api.request.RevokeRequest;
import com.mistra.plank.api.request.SubmitRequest;
import com.mistra.plank.api.response.AuthenticationResponse;
import com.mistra.plank.api.response.GetAssetsResponse;
import com.mistra.plank.api.response.GetDealDataResponse;
import com.mistra.plank.api.response.GetHisDealDataResponse;
import com.mistra.plank.api.response.GetOrdersDataResponse;
import com.mistra.plank.api.response.GetStockListResponse;
import com.mistra.plank.api.response.RevokeResponse;
import com.mistra.plank.api.response.SubmitResponse;
import com.mistra.plank.exception.FieldInputException;
import com.mistra.plank.pojo.model.po.StockSelected;
import com.mistra.plank.pojo.model.po.TradeMethod;
import com.mistra.plank.pojo.model.po.TradeUser;
import com.mistra.plank.pojo.model.vo.AccountVo;
import com.mistra.plank.pojo.model.vo.CommonResponse;
import com.mistra.plank.pojo.model.vo.PageParam;
import com.mistra.plank.pojo.model.vo.PageVo;
import com.mistra.plank.pojo.model.vo.trade.DealVo;
import com.mistra.plank.pojo.model.vo.trade.OrderVo;
import com.mistra.plank.pojo.model.vo.trade.TradeRuleVo;
import com.mistra.plank.service.TradeApiService;
import com.mistra.plank.service.TradeService;
import com.mistra.plank.util.StockUtil;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("trade")
public class TradeController extends BaseController {

    @Autowired
    private TradeApiService tradeApiService;

    @Autowired
    private TradeService tradeService;

    @RequestMapping("queryVerifyCodeUrl")
    public CommonResponse queryVerifyCodeUrl() {
        TradeMethod tradeMethod = tradeService.getTradeMethodByName(BaseTradeRequest.TradeRequestMethod.YZM.value());
        String url = tradeMethod.getUrl();
        return CommonResponse.buildResponse(url);
    }

    @PostMapping("login")
    public CommonResponse login(int userId, String password, String identifyCode, String randNum) {
        AuthenticationRequest request = new AuthenticationRequest(userId);
        request.setPassword(password);
        request.setIdentifyCode(identifyCode);
        request.setRandNumber(randNum);

        TradeResultVo<AuthenticationResponse> resultVo = tradeApiService.authentication(request);
        if (resultVo.success()) {
            AuthenticationResponse response = resultVo.getData().get(0);
            TradeUser tradeUser = new TradeUser();
            tradeUser.setId(request.getUserId());
            tradeUser.setCookie(response.getCookie());
            tradeUser.setValidateKey(response.getValidateKey());
            tradeService.updateTradeUser(tradeUser);
            resultVo.setMessage(CommonResponse.DEFAULT_MESSAGE_SUCCESS);
        }
        return CommonResponse.buildResponse(resultVo.getMessage());
    }

    @RequestMapping("ruleList")
    public PageVo<TradeRuleVo> getRuleList(PageParam pageParam) {
        return tradeService.getTradeRuleList(pageParam);
    }

    @PostMapping("changeRuleState")
    public CommonResponse changeRuleState(int id, int state) {
        FieldInputException e = null;
        if (state != 0 && state != 1) {
            e = new FieldInputException();
            e.addError("state", "state invalid");
        }
        if (id < 0) {
            if (e == null) {
                e = new FieldInputException();
            }
            e.addError("id", "id invalid");
        }
        if (e != null && e.hasErrors()) {
            throw e;
        }
        tradeService.changeTradeRuleState(state, id);
        return CommonResponse.buildResponse(CommonResponse.DEFAULT_MESSAGE_SUCCESS);
    }

    @RequestMapping("resetRule")
    public CommonResponse resetRule(int id) {
        FieldInputException e = null;
        if (id < 0) {
            e = new FieldInputException();
            e.addError("id", "id invalid");
        }
        if (e != null && e.hasErrors()) {
            throw e;
        }
        tradeService.resetRule(id);
        return CommonResponse.buildResponse(CommonResponse.DEFAULT_MESSAGE_SUCCESS);
    }

    @RequestMapping("dealList")
    public PageVo<DealVo> getDealList(PageParam pageParam) {
        GetDealDataRequest request = new GetDealDataRequest(getTradeUserId(pageParam.getTradeUserId()));
        TradeResultVo<GetDealDataResponse> dealData = tradeApiService.getDealData(request);
        if (dealData.success()) {
            List<DealVo> list = tradeService.getTradeDealList(dealData.getData());
            return new PageVo<>(subList(list, pageParam), list.size());
        }
        return new PageVo<>(Collections.emptyList(), 0);
    }

    @RequestMapping("hisDealList")
    public PageVo<DealVo> getHisDealList(PageParam pageParam) {
        GetHisDealDataRequest request = new GetHisDealDataRequest(getTradeUserId(pageParam.getTradeUserId()));
        request.setEt(DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
        Date et = new Date();
        et.setTime(et.getTime() - 15 * 24 * 3600 * 1000);
        request.setSt(DateFormatUtils.format(et, "yyyy-MM-dd"));

        TradeResultVo<GetHisDealDataResponse> dealData = tradeApiService.getHisDealData(request);
        if (dealData.success()) {
            List<DealVo> list = tradeService.getTradeDealList(dealData.getData());
            return new PageVo<>(subList(list, pageParam), list.size());
        }
        return new PageVo<>(Collections.emptyList(), 0);
    }

    @RequestMapping("buy")
    public CommonResponse buy(int amount, double price, String stockCode, String stockName, Integer tradeUserId) {
        SubmitRequest request = new SubmitRequest(getTradeUserId(tradeUserId));
        request.setAmount(amount);
        request.setPrice(price);
        request.setStockCode(stockCode);
        request.setZqmc(stockName);
        request.setTradeType(SubmitRequest.B);
        request.setMarket(StockUtil.getStockMarket(request.getStockCode()));
        TradeResultVo<SubmitResponse> response = tradeApiService.submit(request);
        String message = response.getMessage();
        if (response.success()) {
            message = response.getData().get(0).getWtbh();
        }

        return CommonResponse.buildResponse(message);
    }

    @RequestMapping("sale")
    public CommonResponse sale(int amount, double price, String stockCode, String stockName, Integer tradeUserId) {
        SubmitRequest request = new SubmitRequest(getTradeUserId(tradeUserId));
        request.setAmount(amount);
        request.setPrice(price);
        request.setStockCode(stockCode);
        request.setZqmc(stockName);
        request.setTradeType(SubmitRequest.S);
        request.setMarket(StockUtil.getStockMarket(request.getStockCode()));
        TradeResultVo<SubmitResponse> response = tradeApiService.submit(request);
        String message = response.getMessage();
        if (response.success()) {
            message = response.getData().get(0).getWtbh();
        }

        return CommonResponse.buildResponse(message);
    }

    @RequestMapping("orderList")
    public PageVo<OrderVo> getOrderList(PageParam pageParam) {
        GetOrdersDataRequest request = new GetOrdersDataRequest(getTradeUserId(pageParam.getTradeUserId()));
        TradeResultVo<GetOrdersDataResponse> response = tradeApiService.getOrdersData(request);
        if (response.success()) {
            List<OrderVo> list = tradeService.getTradeOrderList(response.getData());
            list = list.stream().filter(v -> v.getState().equals(GetOrdersDataResponse.WEIBAO) || v.getState().equals(GetOrdersDataResponse.YIBAO)).collect(Collectors.toList());
            return new PageVo<>(subList(list, pageParam), list.size());
        }
        return new PageVo<>(Collections.emptyList(), 0);
    }

    @RequestMapping("revoke")
    public CommonResponse revoke(String entrustCode, Integer tradeUserId) {
        RevokeRequest request = new RevokeRequest(getTradeUserId(tradeUserId));
        String revokes = String.format("%s_%s", DateFormatUtils.format(new Date(), "yyyyMMdd"), entrustCode);
        request.setRevokes(revokes);
        TradeResultVo<RevokeResponse> response = tradeApiService.revoke(request);
        return CommonResponse.buildResponse(response.getMessage());
    }

    @RequestMapping("queryAccount")
    public AccountVo queryAccount(Integer tradeUserId) {
        GetAssetsRequest request = new GetAssetsRequest(getTradeUserId(tradeUserId));
        TradeResultVo<GetAssetsResponse> tradeResultVo = tradeApiService.getAsserts(request);
        AccountVo accountVo = new AccountVo();
        if (tradeResultVo.success()) {
            List<GetAssetsResponse> data = tradeResultVo.getData();
            GetAssetsResponse response = data.get(0);
            accountVo.setAvailableAmount(new BigDecimal(response.getKyzj()));
            accountVo.setFrozenAmount(new BigDecimal(response.getDjzj()));
            accountVo.setTotalAmount(new BigDecimal(response.getZzc()));
            accountVo.setWithdrawableAmount(new BigDecimal(response.getKqzj()));
        } else {
            accountVo.setAvailableAmount(BigDecimal.ZERO);
            accountVo.setFrozenAmount(BigDecimal.ZERO);
            accountVo.setTotalAmount(BigDecimal.ZERO);
            accountVo.setWithdrawableAmount(BigDecimal.ZERO);
        }
        return accountVo;
    }

}
