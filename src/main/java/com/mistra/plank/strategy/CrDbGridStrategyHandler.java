package com.mistra.plank.strategy;

import com.mistra.plank.tradeapi.TradeResultVo;
import com.mistra.plank.tradeapi.request.CrSubmitRequest;
import com.mistra.plank.tradeapi.request.SubmitRequest;
import com.mistra.plank.tradeapi.response.CrSubmitResponse;
import com.mistra.plank.tradeapi.response.SubmitResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component("crDbGridStrategyHandler")
public class CrDbGridStrategyHandler extends CrGridStrategyHandler {
    
    @Override
    protected TradeResultVo<SubmitResponse> submit(SubmitRequest request) {
        CrSubmitRequest crRequest = new CrSubmitRequest(request.getUserId());
        BeanUtils.copyProperties(request, crRequest);

        if (SubmitRequest.B.equals(request.getTradeType())) {
            crRequest.setTradeInfo(CrSubmitRequest.xyjylx_db_b);
        } else {
            crRequest.setTradeInfo(CrSubmitRequest.xyjylx_db_s);
        }

        TradeResultVo<CrSubmitResponse> tradeResultVo = tradeApiService.crSubmit(crRequest);
        return buildResult(tradeResultVo);
    }
}
