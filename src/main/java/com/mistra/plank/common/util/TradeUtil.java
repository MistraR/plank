package com.mistra.plank.common.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.mistra.plank.tradeapi.response.CrGetDealDataResponse;
import com.mistra.plank.tradeapi.response.GetDealDataResponse;
import org.springframework.beans.BeanUtils;

public class TradeUtil {

    private TradeUtil() {
    }

    /**
     * merge the partial-deal list
     */
    public static List<GetDealDataResponse> mergeDealList(List<? extends GetDealDataResponse> list) {
        LinkedHashMap<String, GetDealDataResponse> map = new LinkedHashMap<>();
        for (GetDealDataResponse dealResponse : list) {
            String wtbh = dealResponse.getWtbh();
            if (dealResponse instanceof CrGetDealDataResponse) {
                wtbh = ((CrGetDealDataResponse) dealResponse).getWtxh();
            }
            GetDealDataResponse summaryResponse = map.get(wtbh);
            if (summaryResponse == null) {
                summaryResponse = mergeDeal(null, dealResponse);
                map.put(wtbh, summaryResponse);
            } else {
                mergeDeal(summaryResponse, dealResponse);
            }
        }
        return map.values().stream().filter(v -> v.getCjsl().equals(v.getWtsl())).collect(Collectors.toList());
    }

    private static GetDealDataResponse mergeDeal(GetDealDataResponse response, GetDealDataResponse dealResponse) {
        if (response == null) {
            if (dealResponse instanceof CrGetDealDataResponse) {
                response = new CrGetDealDataResponse();
            } else {
                response = new GetDealDataResponse();
            }
            BeanUtils.copyProperties(dealResponse, response);
        } else {
            int cjsl = Integer.parseInt(response.getCjsl());
            int cjsl2 = Integer.parseInt(dealResponse.getCjsl());
            response.setCjsl(String.valueOf(cjsl + cjsl2));
        }
        return response;
    }

}
