package com.mistra.plank.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.common.exception.FieldInputException;
import com.mistra.plank.common.util.StockUtil;
import com.mistra.plank.dao.HoldSharesMapper;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.job.StockProcessor;
import com.mistra.plank.model.dto.StockRealTimePrice;
import com.mistra.plank.model.entity.HoldShares;
import com.mistra.plank.model.entity.Stock;
import com.mistra.plank.model.entity.StockSelected;
import com.mistra.plank.model.entity.TradeMethod;
import com.mistra.plank.model.entity.TradeUser;
import com.mistra.plank.model.enums.AutomaticTradingEnum;
import com.mistra.plank.model.vo.AccountVo;
import com.mistra.plank.model.vo.CommonResponse;
import com.mistra.plank.model.vo.PageParam;
import com.mistra.plank.model.vo.PageVo;
import com.mistra.plank.model.vo.trade.DealVo;
import com.mistra.plank.model.vo.trade.OrderVo;
import com.mistra.plank.model.vo.trade.StockVo;
import com.mistra.plank.model.vo.trade.TradeRuleVo;
import com.mistra.plank.service.StockSelectedService;
import com.mistra.plank.service.TradeApiService;
import com.mistra.plank.service.TradeService;
import com.mistra.plank.tradeapi.TradeResultVo;
import com.mistra.plank.tradeapi.request.AuthenticationRequest;
import com.mistra.plank.tradeapi.request.BaseTradeRequest;
import com.mistra.plank.tradeapi.request.GetAssetsRequest;
import com.mistra.plank.tradeapi.request.GetDealDataRequest;
import com.mistra.plank.tradeapi.request.GetHisDealDataRequest;
import com.mistra.plank.tradeapi.request.GetOrdersDataRequest;
import com.mistra.plank.tradeapi.request.GetStockListRequest;
import com.mistra.plank.tradeapi.request.RevokeRequest;
import com.mistra.plank.tradeapi.request.SubmitRequest;
import com.mistra.plank.tradeapi.response.AuthenticationResponse;
import com.mistra.plank.tradeapi.response.GetAssetsResponse;
import com.mistra.plank.tradeapi.response.GetDealDataResponse;
import com.mistra.plank.tradeapi.response.GetHisDealDataResponse;
import com.mistra.plank.tradeapi.response.GetOrdersDataResponse;
import com.mistra.plank.tradeapi.response.GetStockListResponse;
import com.mistra.plank.tradeapi.response.RevokeResponse;
import com.mistra.plank.tradeapi.response.SubmitResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("trade")
public class TradeController extends BaseController {

    @Autowired
    private TradeApiService tradeApiService;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private StockSelectedService stockSelectedService;

    @Autowired
    private HoldSharesMapper holdSharesMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private PlankConfig plankConfig;

    @Autowired
    private StockProcessor stockProcessor;

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
            tradeUser.setId((long) request.getUserId());
            tradeUser.setCookie(response.getCookie());
            tradeUser.setValidateKey(response.getValidateKey());
            tradeService.updateTradeUser(tradeUser);
            resultVo.setMessage(CommonResponse.DEFAULT_MESSAGE_SUCCESS);
        }
        return CommonResponse.buildResponse(resultVo.getMessage());
    }

    @RequestMapping("stockList")
    public PageVo<StockVo> getStockList(PageParam pageParam) {
        GetStockListRequest request = new GetStockListRequest(getTradeUserId(pageParam.getTradeUserId()));
        TradeResultVo<GetStockListResponse> response = tradeApiService.getStockList(request);
        ArrayList<StockVo> list = new ArrayList<>();
        if (response.success()) {
            list.addAll(tradeService.getTradeStockList(response.getData()));
        }
        List<StockSelected> selectList = stockSelectedService.getList();
        selectList = selectList.stream().filter(v -> list.stream().noneMatch(vo -> vo.getStockCode().equals(v.getCode()))).collect(Collectors.toList());
        list.addAll(tradeService.getTradeStockListBySelected(selectList));
        list.sort((a, b) -> Integer.compare(b.getTotalVolume(), a.getTotalVolume()));
        return new PageVo<>(list, list.size());
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
        log.info("手动买入 {} {}股", stockName, amount);
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
            Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getName, stockName));
            if (Objects.nonNull(stock)) {
                HoldShares holdShare = HoldShares.builder().buyTime(new Date()).clearance(false).code(stock.getCode())
                        .name(stock.getName()).availableVolume(0).number(amount).profit(new BigDecimal(0))
                        // 设置触发止损价
                        .stopLossPrice(BigDecimal.valueOf(price * plankConfig.getStopLossRate()).setScale(2, RoundingMode.HALF_UP))
                        // 设置触发止盈价
                        .takeProfitPrice(BigDecimal.valueOf(price * plankConfig.getTakeProfitRate()).setScale(2, RoundingMode.HALF_UP))
                        .automaticTradingType(AutomaticTradingEnum.MANUAL.name()).buyPrice(BigDecimal.valueOf(price))
                        .highestProfitRatio(new BigDecimal(0)).build();
                holdSharesMapper.insert(holdShare);
                stock.setShareholding(true);
                stock.setBuyTime(new Date());
                stockMapper.updateById(stock);
            }
        }
        return CommonResponse.buildResponse(message);
    }

    @RequestMapping("sale")
    public CommonResponse sale(int amount, double price, String stockCode, String stockName, Integer tradeUserId) {
        HoldShares holdShare = holdSharesMapper.selectOne(new QueryWrapper<HoldShares>().eq("name", stockName)
                .ge("available_volume", 0));
        log.info("手动卖出 {} {}股", stockName, amount);
        SubmitRequest request = new SubmitRequest(getTradeUserId(tradeUserId));
        StockRealTimePrice stockRealTimePrice = stockProcessor.getStockRealTimePriceByCode(holdShare.getCode());
        request.setAmount(Math.min(amount, holdShare.getAvailableVolume()));
        request.setPrice(BigDecimal.valueOf(stockRealTimePrice.getCurrentPrice() * 0.985).setScale(2, RoundingMode.HALF_UP).doubleValue());
        request.setStockCode(stockCode);
        request.setZqmc(stockName);
        request.setTradeType(SubmitRequest.S);
        request.setMarket(StockUtil.getStockMarket(request.getStockCode()));
        TradeResultVo<SubmitResponse> response = tradeApiService.submit(request);
        String message = response.getMessage();
        if (response.success()) {
            message = response.getData().get(0).getWtbh();
            // WEB页面手动卖出的，同时更新掉数据库持仓表的数据
            holdShare.setAvailableVolume(holdShare.getAvailableVolume() - request.getAmount());
            holdShare.setProfit(holdShare.getProfit().add(BigDecimal.valueOf((stockRealTimePrice.getCurrentPrice() - holdShare.getBuyPrice().doubleValue()) * request.getAmount())));
            holdShare.setClearance(holdShare.getAvailableVolume() == 0);
            holdShare.setSaleTime(new Date());
            holdSharesMapper.updateById(holdShare);
            Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getName, stockName));
            stock.setShareholding(false);
            stockMapper.updateById(stock);
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
