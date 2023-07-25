package com.mistra.plank.common.util;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.web.client.ResourceAccessException;
import com.alibaba.fastjson.JSON;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2022/5/7
 */
public class HttpUtil {

    public static CloseableHttpResponse sendGetResponse(CloseableHttpClient httpClient, String url) throws IOException {
        return httpClient.execute(new HttpGet(url));
    }

    public static String sendGet(CloseableHttpClient httpClient, String url) {
        return HttpUtil.sendGet(httpClient, url, null, null);
    }

    public static String sendGet(CloseableHttpClient httpClient, String url, Map<String, String> header) {
        return HttpUtil.sendGet(httpClient, url, header, null);
    }

    public static String sendGet(CloseableHttpClient httpClient, String url, String charset) {
        return HttpUtil.sendGet(httpClient, url, null, charset);
    }

    public static String sendGet(CloseableHttpClient httpClient, String url, Map<String, String> header, String charset) {
        HttpGet httpGet = HttpUtil.getHttpGet(url, header);
        if (charset == null) {
            charset = Consts.UTF_8.name();
        }
        return HttpUtil.sendRequest(httpClient, httpGet, charset);
    }

    public static String sendPost(CloseableHttpClient httpClient, String url, Map<String, Object> params) {
        return HttpUtil.sendPost(httpClient, url, params, null);
    }

    public static String sendPost(CloseableHttpClient httpClient, String url, Map<String, Object> params, Map<String, String> header) {
        HttpPost httpPost = HttpUtil.getHttpPost(url, header);
        return HttpUtil.sendEntityRequest(httpClient, httpPost, params);
    }

    public static String sendPostJson(CloseableHttpClient httpClient, String url, Map<String, Object> params) {
        return HttpUtil.sendPostJson(httpClient, url, params, null);
    }

    public static String sendPostJson(CloseableHttpClient httpClient, String url, List<Map<String, Object>> params) {
        return HttpUtil.sendPostJson(httpClient, url, params, null);
    }

    public static String sendPostJson(CloseableHttpClient httpClient, String url, List<Map<String, Object>> params, Map<String, String> header) {
        return sendPostJsonIn(httpClient, url, params, header);
    }

    public static String sendPostJson(CloseableHttpClient httpClient, String url, Map<String, Object> params, Map<String, String> header) {
        return sendPostJsonIn(httpClient, url, params, header);
    }

    private static String sendPostJsonIn(CloseableHttpClient httpClient, String url, Object params, Map<String, String> header) {
        HttpPost httpPost = HttpUtil.getHttpPost(url, header);
        httpPost.addHeader("Content-type", "application/json; charset=utf-8");
        return HttpUtil.sendStringEntityRequest(httpClient, httpPost, params);
    }

    private static String sendEntityRequest(CloseableHttpClient httpClient, HttpEntityEnclosingRequestBase request, Map<String, Object> params) {
        if (params != null) {
            List<BasicNameValuePair> parameters = params.entrySet().stream().map(entry ->
                    new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue()))
            ).collect(Collectors.toList());

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(parameters, Consts.UTF_8);
            request.setEntity(entity);
        }
        return HttpUtil.sendRequest(httpClient, request, Consts.UTF_8.name());
    }

    private static String sendStringEntityRequest(CloseableHttpClient httpClient, HttpEntityEnclosingRequestBase request, Object params) {
        if (params != null) {
            String json = JSON.toJSONString(params);
            StringEntity entity = new StringEntity(json, Consts.UTF_8);
            request.setEntity(entity);
        }
        return HttpUtil.sendRequest(httpClient, request, Consts.UTF_8.name());
    }

    private static String sendRequest(CloseableHttpClient httpClient, HttpUriRequest request, String charset) {
        if (!request.containsHeader("User-Agent")) {
            request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36");
        }
        DefaultHttpClient wrappedClient = WebClientDevWrapper.wrapClient(httpClient,request);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            return EntityUtils.toString(response.getEntity(), charset);
        } catch (IOException ex) {
            throw new ResourceAccessException("I/O error on " + request.getMethod() + " request for \""
                    + request.getURI() + "\": " + ex.getMessage(), ex);
        }
    }

    private static HttpGet getHttpGet(String url, Map<String, String> header) {
        HttpGet httpGet = new HttpGet(url);
        if (header != null) {
            header.forEach(httpGet::addHeader);
        }
        RequestConfig requestConfig = RequestConfig.custom().build();
        httpGet.setConfig(requestConfig);
        return httpGet;
    }

    private static HttpPost getHttpPost(String url, Map<String, String> header) {
        HttpPost httpPost = new HttpPost(url);
        if (header != null) {
            header.forEach(httpPost::addHeader);
        }
        RequestConfig requestConfig = RequestConfig.custom().build();
        httpPost.setConfig(requestConfig);
        return httpPost;
    }

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
