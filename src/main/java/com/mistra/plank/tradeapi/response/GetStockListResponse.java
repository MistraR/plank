package com.mistra.plank.tradeapi.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetStockListResponse extends BaseTradeResponse {

    /**
     * 证券名称
     */
    private String Zqmc;
    /**
     * 证券代码
     */
    private String Zqdm;
    /**
     * 证券数量
     */
    private String Zqsl;
    /**
     * 证券价格
     */
    private String Zxjg;
    /**
     * 可用数量
     */
    private String Kysl;
    /**
     * 成本价
     */
    private String Cbjg;
    /**
     * 利润
     */
    private String Ljyk;

}
