package com.mistra.plank.service;

import com.alibaba.fastjson.TypeReference;
import com.mistra.plank.tradeapi.TradeResultVo;
import com.mistra.plank.tradeapi.request.BaseTradeRequest;
import com.mistra.plank.tradeapi.request.CrGetCanBuyNewStockListV3Request;
import com.mistra.plank.tradeapi.request.CrGetConvertibleBondListV2Request;
import com.mistra.plank.tradeapi.request.CrGetDealDataRequest;
import com.mistra.plank.tradeapi.request.CrGetHisDealDataRequest;
import com.mistra.plank.tradeapi.request.CrGetHisOrdersDataRequest;
import com.mistra.plank.tradeapi.request.CrGetOrdersDataRequest;
import com.mistra.plank.tradeapi.request.CrGetRzrqAssertsRequest;
import com.mistra.plank.tradeapi.request.CrQueryCollateralRequest;
import com.mistra.plank.tradeapi.request.CrRevokeRequest;
import com.mistra.plank.tradeapi.request.CrSubmitBatTradeV2Request;
import com.mistra.plank.tradeapi.request.CrSubmitRequest;
import com.mistra.plank.tradeapi.request.GetAssetsRequest;
import com.mistra.plank.tradeapi.request.GetCanBuyNewStockListV3Request;
import com.mistra.plank.tradeapi.request.GetConvertibleBondListV2Request;
import com.mistra.plank.tradeapi.request.GetDealDataRequest;
import com.mistra.plank.tradeapi.request.GetHisDealDataRequest;
import com.mistra.plank.tradeapi.request.GetHisOrdersDataRequest;
import com.mistra.plank.tradeapi.request.GetOrdersDataRequest;
import com.mistra.plank.tradeapi.request.GetStockListRequest;
import com.mistra.plank.tradeapi.request.RevokeRequest;
import com.mistra.plank.tradeapi.request.SubmitBatTradeV2Request;
import com.mistra.plank.tradeapi.request.SubmitRequest;
import com.mistra.plank.tradeapi.response.BaseTradeResponse;
import com.mistra.plank.tradeapi.response.CrGetCanBuyNewStockListV3Response;
import com.mistra.plank.tradeapi.response.CrGetConvertibleBondListV2Response;
import com.mistra.plank.tradeapi.response.CrGetDealDataResponse;
import com.mistra.plank.tradeapi.response.CrGetHisDealDataResponse;
import com.mistra.plank.tradeapi.response.CrGetHisOrdersDataResponse;
import com.mistra.plank.tradeapi.response.CrGetOrdersDataResponse;
import com.mistra.plank.tradeapi.response.CrGetRzrqAssertsResponse;
import com.mistra.plank.tradeapi.response.CrQueryCollateralResponse;
import com.mistra.plank.tradeapi.response.CrRevokeResponse;
import com.mistra.plank.tradeapi.response.CrSubmitBatTradeV2Response;
import com.mistra.plank.tradeapi.response.CrSubmitResponse;
import com.mistra.plank.tradeapi.response.GetAssetsResponse;
import com.mistra.plank.tradeapi.response.GetCanBuyNewStockListV3Response;
import com.mistra.plank.tradeapi.response.GetConvertibleBondListV2Response;
import com.mistra.plank.tradeapi.response.GetDealDataResponse;
import com.mistra.plank.tradeapi.response.GetHisDealDataResponse;
import com.mistra.plank.tradeapi.response.GetHisOrdersDataResponse;
import com.mistra.plank.tradeapi.response.GetOrdersDataResponse;
import com.mistra.plank.tradeapi.response.GetStockListResponse;
import com.mistra.plank.tradeapi.response.RevokeResponse;
import com.mistra.plank.tradeapi.response.SubmitBatTradeV2Response;
import com.mistra.plank.tradeapi.response.SubmitResponse;

public abstract class AbstractTradeApiService implements TradeApiService {

    @Override
    public TradeResultVo<GetAssetsResponse> getAsserts(GetAssetsRequest request) {
        return send(request, new TypeReference<GetAssetsResponse>() {
        });
    }

    @Override
    public TradeResultVo<SubmitResponse> submit(SubmitRequest request) {
        return send(request, new TypeReference<SubmitResponse>() {
        });
    }

    @Override
    public TradeResultVo<RevokeResponse> revoke(RevokeRequest request) {
        return send(request, new TypeReference<RevokeResponse>() {
        });
    }

    @Override
    public TradeResultVo<GetStockListResponse> getStockList(GetStockListRequest request) {
        return send(request, new TypeReference<GetStockListResponse>() {
        });
    }

    @Override
    public TradeResultVo<GetOrdersDataResponse> getOrdersData(GetOrdersDataRequest request) {
        return send(request, new TypeReference<GetOrdersDataResponse>() {
        });
    }

    @Override
    public TradeResultVo<GetDealDataResponse> getDealData(GetDealDataRequest request) {
        return send(request, new TypeReference<GetDealDataResponse>() {
        });
    }

    @Override
    public TradeResultVo<GetHisDealDataResponse> getHisDealData(GetHisDealDataRequest request) {
        return send(request, new TypeReference<GetHisDealDataResponse>() {
        });
    }

    @Override
    public TradeResultVo<GetHisOrdersDataResponse> getHisOrdersData(GetHisOrdersDataRequest request) {
        return send(request, new TypeReference<GetHisOrdersDataResponse>() {
        });
    }

    @Override
    public TradeResultVo<GetCanBuyNewStockListV3Response> getCanBuyNewStockListV3(GetCanBuyNewStockListV3Request request) {
        return send(request, new TypeReference<GetCanBuyNewStockListV3Response>() {
        });
    }

    @Override
    public TradeResultVo<GetConvertibleBondListV2Response> getConvertibleBondListV2(GetConvertibleBondListV2Request request) {
        return send(request, new TypeReference<GetConvertibleBondListV2Response>() {
        });
    }

    @Override
    public TradeResultVo<SubmitBatTradeV2Response> submitBatTradeV2(SubmitBatTradeV2Request request) {
        return send(request, new TypeReference<SubmitBatTradeV2Response>() {
        });
    }

    @Override
    public TradeResultVo<CrGetRzrqAssertsResponse> crGetRzrqAsserts(CrGetRzrqAssertsRequest request) {
        return send(request, new TypeReference<CrGetRzrqAssertsResponse>() {
        });
    }

    @Override
    public TradeResultVo<CrQueryCollateralResponse> crQueryCollateral(CrQueryCollateralRequest request) {
        return send(request, new TypeReference<CrQueryCollateralResponse>() {
        });
    }

    @Override
    public TradeResultVo<CrSubmitResponse> crSubmit(CrSubmitRequest request) {
        return send(request, new TypeReference<CrSubmitResponse>() {
        });
    }

    @Override
    public TradeResultVo<CrRevokeResponse> crRevoke(CrRevokeRequest request) {
        return send(request, new TypeReference<CrRevokeResponse>() {
        });
    }

    @Override
    public TradeResultVo<CrGetOrdersDataResponse> crGetOrdersData(CrGetOrdersDataRequest request) {
        return send(request, new TypeReference<CrGetOrdersDataResponse>() {
        });
    }


    @Override
    public TradeResultVo<CrGetDealDataResponse> crGetDealData(CrGetDealDataRequest request) {
        return send(request, new TypeReference<CrGetDealDataResponse>() {
        });
    }

    @Override
    public TradeResultVo<CrGetHisDealDataResponse> crGetHisDealData(CrGetHisDealDataRequest request) {
        return send(request, new TypeReference<CrGetHisDealDataResponse>() {
        });
    }

    @Override
    public TradeResultVo<CrGetHisOrdersDataResponse> crGetHisOrdersData(CrGetHisOrdersDataRequest request) {
        return send(request, new TypeReference<CrGetHisOrdersDataResponse>() {
        });
    }

    @Override
    public TradeResultVo<CrGetCanBuyNewStockListV3Response> crGetCanBuyNewStockListV3(CrGetCanBuyNewStockListV3Request request) {
        return send(request, new TypeReference<CrGetCanBuyNewStockListV3Response>() {
        });
    }

    @Override
    public TradeResultVo<CrGetConvertibleBondListV2Response> crGetConvertibleBondListV2(CrGetConvertibleBondListV2Request request) {
        return send(request, new TypeReference<CrGetConvertibleBondListV2Response>() {
        });
    }

    @Override
    public TradeResultVo<CrSubmitBatTradeV2Response> crSubmitBatTradeV2(CrSubmitBatTradeV2Request request) {
        return send(request, new TypeReference<CrSubmitBatTradeV2Response>() {
        });
    }

    public abstract <T extends BaseTradeResponse> TradeResultVo<T> send(BaseTradeRequest request, TypeReference<T> responseType);

}
