package com.mistra.plank.model.param;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author rui.wang
 * @ Version: 1.0
 * @ Time: 2022/11/16 11:43
 * @ Description:
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SelfSelectParam {

    private List<String> names;

}
