package com.mistra.plank.common.util;

import java.util.Collection;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2022/6/15
 */
public class StringUtil {

    public static String collectionToString(Collection collection) {
        return collection.toString().replace(" ", "").replace("[", "")
                .replace("]", "");
    }
}
