package com.mistra.plank.config;

import org.springframework.stereotype.Component;

/**
 * @author rui.wang
 * @ Version: 1.0
 * @ Time: 2023/2/13 17:25
 * @ Description:
 */
@Component
public class SystemConstant {

    public static final Integer W = 10000;

    /**
     * 成交额 过滤金额 >3亿
     */
    public static final Integer TRANSACTION_AMOUNT_FILTER = 300000000;

}
