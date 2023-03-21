package com.mistra.plank.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.mistra.plank.common.util.RSAUtil;
import com.mistra.plank.model.entity.TradeMethod;
import com.mistra.plank.model.entity.TradeUser;
import com.mistra.plank.service.AbstractTradeApiService;
import com.mistra.plank.service.TradeService;
import com.mistra.plank.tradeapi.TradeClient;
import com.mistra.plank.tradeapi.TradeResultVo;
import com.mistra.plank.tradeapi.request.AuthenticationRequest;
import com.mistra.plank.tradeapi.request.BaseTradeListRequest;
import com.mistra.plank.tradeapi.request.BaseTradeRequest;
import com.mistra.plank.tradeapi.response.AuthenticationResponse;
import com.mistra.plank.tradeapi.response.BaseTradeResponse;
import org.apache.commons.beanutils.BeanMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TradeApiServiceImpl extends AbstractTradeApiService {

    private final Logger logger = LoggerFactory.getLogger(TradeApiServiceImpl.class);

    //    @Value(value = "${emSecSecurityServerUrl}")
    private String emSecSecurityServerUrl;

    private static final List<String> IgnoreList = Arrays.asList("class", "userId", "method");

    public static class TradeResult<T> {
        private String Message;
        private int Status;
        private T Data;

        public String getMessage() {
            return Message;
        }

        public void setMessage(String message) {
            Message = message;
        }

        public int getStatus() {
            return Status;
        }

        public void setStatus(int status) {
            Status = status;
        }

        public T getData() {
            return Data;
        }

        public void setData(T data) {
            Data = data;
        }
    }

    private final ResponseParser dataObjReponseParser = new ResponseParser() {
        @Override
        public <T> TradeResultVo<T> parse(String content, TypeReference<T> responseType) {
            TradeResultVo<T> resultVo = new TradeResultVo<>();
            ArrayList<T> newList = new ArrayList<>();

            TradeResult<T> result = JSON.parseObject(content, new TypeReference<TradeResult<T>>() {
            });
            if (TradeResultVo.success(result.getStatus())) {
                String text = JSON.toJSONString(result.Data);
                T t = JSON.parseObject(text, responseType);
                newList.add(t);
            } else {
                resultVo.setData(Collections.emptyList());
            }

            resultVo.setMessage(result.getMessage());
            resultVo.setStatus(result.getStatus());
            resultVo.setData(newList);
            return resultVo;
        }

        @Override
        public int version() {
            return BaseTradeRequest.VERSION_DATA_OBJ;
        }

    };

    private final ResponseParser msgResponseParser = new ResponseParser() {
        @Override
        public <T> TradeResultVo<T> parse(String content, TypeReference<T> responseType) {
            TradeResultVo<T> resultVo = new TradeResultVo<>();
            resultVo.setData(Collections.emptyList());
            resultVo.setStatus(TradeResultVo.STATUS_SUCCESS);
            resultVo.setMessage(content);
            return resultVo;
        }

        @Override
        public int version() {
            return BaseTradeRequest.VERSION_MSG;
        }
    };

    private final ResponseParser objReponseParser = new ResponseParser() {
        @Override
        public <T> TradeResultVo<T> parse(String content, TypeReference<T> responseType) {
            T t = JSON.parseObject(content, responseType);
            ArrayList<T> newList = new ArrayList<>();
            newList.add(t);

            TradeResultVo<T> resultVo = new TradeResultVo<>();
            resultVo.setData(newList);

            return resultVo;
        }

        @Override
        public int version() {
            return BaseTradeRequest.VERSION_OBJ;
        }
    };

    private final ResponseParser dataListReponseParser = new ResponseParser() {
        @Override
        public <T> TradeResultVo<T> parse(String content, TypeReference<T> responseType) {
            TradeResultVo<T> resultVo = JSON.parseObject(content, new TypeReference<TradeResultVo<T>>() {
            });
            if (resultVo.success()) {
                List<T> list = resultVo.getData();
                ArrayList<T> newList = new ArrayList<>();
                if (list != null) {
                    list.forEach(d -> {
                        String text = JSON.toJSONString(d);
                        T t = JSON.parseObject(text, responseType);
                        newList.add(t);
                    });
                }
                resultVo.setData(newList);
            } else {
                resultVo.setData(Collections.emptyList());
            }
            return resultVo;
        }

        @Override
        public int version() {
            return BaseTradeRequest.VERSION_DATA_LIST;
        }
    };

    @Autowired
    private TradeService tradeService;

    @Autowired
    private TradeClient tradeClient;

    @Override
    public <T extends BaseTradeResponse> TradeResultVo<T> send(BaseTradeRequest request, TypeReference<T> responseType) {
        ResponseParser responseParse = getResponseParser(request);

        String url = getUrl(request);
        logger.debug("trade {} url: {}", request.getMethod(), url);
        Map<String, String> header = getHeader(request);

        List<Map<String, Object>> paramList = null;
        Map<String, Object> params = null;

        boolean isSendList = request instanceof BaseTradeListRequest;
        if (isSendList) {
            paramList = ((BaseTradeListRequest) request).getList().stream().map(this::getParams).collect(Collectors.toList());
            logger.debug("trade {} request: {}", request.getMethod(), paramList);
        } else {
            params = getParams(request);
            logger.debug("trade {} request: {}", request.getMethod(), params);
        }

        String content;
        if (isSendList) {
            content = tradeClient.sendListJson(url, paramList, header);
        } else {
            content = tradeClient.send(url, params, header);
        }
        logger.debug("trade {} response: {}", request.getMethod(), content);
        return responseParse.parse(content, responseType);
    }

    @Override
    public TradeResultVo<AuthenticationResponse> authentication(AuthenticationRequest request) {
        TradeMethod tradeMethod = tradeService.getTradeMethodByName(request.getMethod());
        TradeUser tradeUser = tradeService.getTradeUserById(request.getUserId());

        setRequestSecInfo(request);
        request.setPassword(encodePassword(request.getPassword()));

        Map<String, String> header = getHeader(request);

        if (!StringUtils.hasLength(request.getSecInfo())) {
            logger.info("authentication use mac User-Agent");
            header.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/536.66");
        }

        Map<String, Object> params = getParams(request);
        params.put("userId", tradeUser.getAccountId());
        try {
            tradeClient.openSession();
            String content = tradeClient.sendNewInstance(tradeMethod.getUrl(), params, header);
            TradeResultVo<AuthenticationResponse> resultVo = dataListReponseParser.parse(content, new TypeReference<AuthenticationResponse>() {
            });
            if (resultVo.success()) {
                TradeMethod authCheckTradeMethod = tradeService.getTradeMethodByName(BaseTradeRequest.TradeRequestMethod.AuthenticationCheck.value());
                AuthenticationResponse response = new AuthenticationResponse();
                String cookie = tradeClient.getCurrentCookie();
                response.setCookie(cookie);

                String content2 = tradeClient.sendNewInstance(authCheckTradeMethod.getUrl(), new HashMap<>(), header);
                String validateKey = getValidateKey(content2);

                response.setValidateKey(validateKey);
                resultVo.setData(Collections.singletonList(response));
            }
            return resultVo;
        } finally {
            tradeClient.destoryCurrentSession();
        }
    }

    private void setRequestSecInfo(AuthenticationRequest request) {
        if (emSecSecurityServerUrl == null) {
            return;
        }

        try {
            tradeClient.openSession();
            String content = tradeClient.sendNewInstance(emSecSecurityServerUrl + request.getIdentifyCode(), null, null);
            @SuppressWarnings("unchecked")
            Map<String, String> map = JSON.parseObject(content, Map.class);
            String userInfo = map.get("userInfo");
            request.setSecInfo(userInfo);
        } catch (Exception e) {
            logger.info("not code get from EM SecSecurity Server");
        } finally {
            tradeClient.destoryCurrentSession();
        }
    }

    private String encodePassword(String password) {
        if (password.length() != 6) {
            return password;
        }
        String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDHdsyxT66pDG4p73yope7jxA92\nc0AT4qIJ/xtbBcHkFPK77upnsfDTJiVEuQDH+MiMeb+XhCLNKZGp0yaUU6GlxZdp\n+nLW8b7Kmijr3iepaDhcbVTsYBWchaWUXauj9Lrhz58/6AE/NF0aMolxIGpsi+ST\n2hSHPu3GSXMdhPCkWQIDAQAB";
        return RSAUtil.encodeWithPublicKey(password, publicKey);
    }

    private String getValidateKey(String content) {
        String key = "input id=\"em_validatekey\" type=\"hidden\" value=\"";
        int inputBegin = content.indexOf(key) + key.length();
        int inputEnd = content.indexOf("\" />", inputBegin);
        return content.substring(inputBegin, inputEnd);
    }

    private ResponseParser getResponseParser(BaseTradeRequest request) {
        if (request.responseVersion() == dataObjReponseParser.version()) {
            return dataObjReponseParser;
        }
        if (request.responseVersion() == msgResponseParser.version()) {
            return msgResponseParser;
        }
        if (request.responseVersion() == objReponseParser.version()) {
            return objReponseParser;
        }
        return dataListReponseParser;
    }

    private Map<String, Object> getParams(Object request) {
        Map<Object, Object> beanMap = new BeanMap(request);
        HashMap<String, Object> params = new HashMap<>();
        beanMap.entrySet().stream().filter(entry -> !TradeApiServiceImpl.IgnoreList.contains(String.valueOf(entry.getKey())))
                .forEach(entry -> params.put(String.valueOf(entry.getKey()), entry.getValue()));
        return params;
    }

    private String getUrl(BaseTradeRequest request) {
        TradeMethod tradeMethod = tradeService.getTradeMethodByName(request.getMethod());
        TradeUser tradeUser = tradeService.getTradeUserById(request.getUserId());
        String url = tradeMethod.getUrl();
        return url.replace("${validatekey}", tradeUser.getValidateKey());
    }

    private Map<String, String> getHeader(BaseTradeRequest request) {
        TradeUser tradeUser = tradeService.getTradeUserById(request.getUserId());
        HashMap<String, String> header = new HashMap<>();
        if (!(request instanceof AuthenticationRequest)) {
            header.put("cookie", tradeUser.getCookie());
        }
        header.put("gw_reqtimestamp", System.currentTimeMillis() + "");
        header.put("X-Requested-With", "XMLHttpRequest");
        return header;
    }

    private interface ResponseParser {
        <T> TradeResultVo<T> parse(String content, TypeReference<T> responseType);

        int version();
    }

}
