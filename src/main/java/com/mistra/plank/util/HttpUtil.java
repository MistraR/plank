package com.mistra.plank.util;

import java.net.URI;
import java.util.Objects;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2022/5/7
 */
public class HttpUtil {

    public static String getHttpGetResponseString(String url, String cookie) {
        String body = "";
        try {
            DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(URI.create(url));
            if (Objects.nonNull(cookie)) {
                httpGet.setHeader("Cookie", cookie);
            }
            CloseableHttpResponse response = defaultHttpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                body = EntityUtils.toString(entity, "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return body;
    }
}
