package com.mistra.plank.service;

import com.mistra.plank.api.TradeResultVo;
import com.mistra.plank.api.request.AuthenticationRequest;
import com.mistra.plank.api.request.CrGetCanBuyNewStockListV3Request;
import com.mistra.plank.api.request.CrGetConvertibleBondListV2Request;
import com.mistra.plank.api.request.CrGetDealDataRequest;
import com.mistra.plank.api.request.CrGetHisDealDataRequest;
import com.mistra.plank.api.request.CrGetHisOrdersDataRequest;
import com.mistra.plank.api.request.CrGetOrdersDataRequest;
import com.mistra.plank.api.request.CrGetRzrqAssertsRequest;
import com.mistra.plank.api.request.CrQueryCollateralRequest;
import com.mistra.plank.api.request.CrRevokeRequest;
import com.mistra.plank.api.request.CrSubmitBatTradeV2Request;
import com.mistra.plank.api.request.CrSubmitRequest;
import com.mistra.plank.api.request.GetAssetsRequest;
import com.mistra.plank.api.request.GetCanBuyNewStockListV3Request;
import com.mistra.plank.api.request.GetConvertibleBondListV2Request;
import com.mistra.plank.api.request.GetDealDataRequest;
import com.mistra.plank.api.request.GetHisDealDataRequest;
import com.mistra.plank.api.request.GetHisOrdersDataRequest;
import com.mistra.plank.api.request.GetOrdersDataRequest;
import com.mistra.plank.api.request.GetStockListRequest;
import com.mistra.plank.api.request.RevokeRequest;
import com.mistra.plank.api.request.SubmitBatTradeV2Request;
import com.mistra.plank.api.request.SubmitRequest;
import com.mistra.plank.api.response.AuthenticationResponse;
import com.mistra.plank.api.response.CrGetCanBuyNewStockListV3Response;
import com.mistra.plank.api.response.CrGetConvertibleBondListV2Response;
import com.mistra.plank.api.response.CrGetDealDataResponse;
import com.mistra.plank.api.response.CrGetHisDealDataResponse;
import com.mistra.plank.api.response.CrGetHisOrdersDataResponse;
import com.mistra.plank.api.response.CrGetOrdersDataResponse;
import com.mistra.plank.api.response.CrGetRzrqAssertsResponse;
import com.mistra.plank.api.response.CrQueryCollateralResponse;
import com.mistra.plank.api.response.CrRevokeResponse;
import com.mistra.plank.api.response.CrSubmitBatTradeV2Response;
import com.mistra.plank.api.response.CrSubmitResponse;
import com.mistra.plank.api.response.GetAssetsResponse;
import com.mistra.plank.api.response.GetCanBuyNewStockListV3Response;
import com.mistra.plank.api.response.GetConvertibleBondListV2Response;
import com.mistra.plank.api.response.GetDealDataResponse;
import com.mistra.plank.api.response.GetHisDealDataResponse;
import com.mistra.plank.api.response.GetHisOrdersDataResponse;
import com.mistra.plank.api.response.GetOrdersDataResponse;
import com.mistra.plank.api.response.GetStockListResponse;
import com.mistra.plank.api.response.RevokeResponse;
import com.mistra.plank.api.response.SubmitBatTradeV2Response;
import com.mistra.plank.api.response.SubmitResponse;

public interface TradeApiService {

    /**
     * 我的资产
     */
    TradeResultVo<GetAssetsResponse> getAsserts(GetAssetsRequest request);

    /**
     * 提交委托
     */
    TradeResultVo<SubmitResponse> submit(SubmitRequest request);

    /**
     * 撤单
     */
    TradeResultVo<RevokeResponse> revoke(RevokeRequest request);

    /**
     * 我的持仓
     */
    TradeResultVo<GetStockListResponse> getStockList(GetStockListRequest request);

    /**
     * 当日委托
     */
    TradeResultVo<GetOrdersDataResponse> getOrdersData(GetOrdersDataRequest request);

    /**
     * 当日成交
     */
    TradeResultVo<GetDealDataResponse> getDealData(GetDealDataRequest request);

    /**
     * 登录
     */
    TradeResultVo<AuthenticationResponse> authentication(AuthenticationRequest request);

    /**
     * 历史成交
     */
    TradeResultVo<GetHisDealDataResponse> getHisDealData(GetHisDealDataRequest request);

    /**
     * 历史委托
     */
    TradeResultVo<GetHisOrdersDataResponse> getHisOrdersData(GetHisOrdersDataRequest request);

    /**
     * 查询可申购新股列表
     */
    TradeResultVo<GetCanBuyNewStockListV3Response> getCanBuyNewStockListV3(GetCanBuyNewStockListV3Request request);

    /**
     * 查询可申购新股列表
     */
    TradeResultVo<GetConvertibleBondListV2Response> getConvertibleBondListV2(GetConvertibleBondListV2Request request);

    /**
     * 批量申购
     */
    TradeResultVo<SubmitBatTradeV2Response> submitBatTradeV2(SubmitBatTradeV2Request request);

    /**
     * 信用我的资产
     */
    TradeResultVo<CrGetRzrqAssertsResponse> crGetRzrqAsserts(CrGetRzrqAssertsRequest request);

    /**
     * 信用我的持仓
     */
    TradeResultVo<CrQueryCollateralResponse> crQueryCollateral(CrQueryCollateralRequest request);

    /**
     * 信用提交委托
     */
    TradeResultVo<CrSubmitResponse> crSubmit(CrSubmitRequest request);

    /**
     * 信用撤单
     */
    TradeResultVo<CrRevokeResponse> crRevoke(CrRevokeRequest request);

    /**
     * 信用当日委托
     */
    TradeResultVo<CrGetOrdersDataResponse> crGetOrdersData(CrGetOrdersDataRequest request);

    /**
     * 信用当日成交
     */
    TradeResultVo<CrGetDealDataResponse> crGetDealData(CrGetDealDataRequest request);

    /**
     * 信用历史成交
     */
    TradeResultVo<CrGetHisDealDataResponse> crGetHisDealData(CrGetHisDealDataRequest request);

    /**
     * 信用历史委托
     */
    TradeResultVo<CrGetHisOrdersDataResponse> crGetHisOrdersData(CrGetHisOrdersDataRequest request);

    /**
     * 信用查询可申购新股列表
     */
    TradeResultVo<CrGetCanBuyNewStockListV3Response> crGetCanBuyNewStockListV3(CrGetCanBuyNewStockListV3Request request);

    /**
     * 信用查询可申购新股列表
     */
    TradeResultVo<CrGetConvertibleBondListV2Response> crGetConvertibleBondListV2(CrGetConvertibleBondListV2Request request);

    /**
     * 信用批量申购
     */
    TradeResultVo<CrSubmitBatTradeV2Response> crSubmitBatTradeV2(CrSubmitBatTradeV2Request request);

}
