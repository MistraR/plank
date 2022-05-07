package com.mistra.plank.pojo.param;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 基金季报持仓导入参数
 *
 * @author mistra@future.com
 * @date 2022/5/7
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FundHoldingsParam {

    /**
     * 东方财富Choice平台导出的Excel
     */
    private MultipartFile file;

    /**
     * 季度
     */
    private Integer quarter;

}
