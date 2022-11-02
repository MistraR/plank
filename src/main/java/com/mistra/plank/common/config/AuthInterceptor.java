package com.mistra.plank.common.config;

import com.mistra.plank.model.entity.User;
import com.mistra.plank.service.UserService;
import com.mistra.plank.common.util.StockConsts;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    @Autowired
    private UserService userService;

    private final Set<String> openUrlList = new HashSet<>();

    public AuthInterceptor() {
        openUrlList.add("/user/login");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
//        int userId = getUserId(request);
        return handleAuth(request, response, 1);
    }

    private boolean handleAuth(HttpServletRequest request, HttpServletResponse response, int userId)
            throws IOException {
        String url = request.getRequestURI();
        String addr = request.getRemoteAddr();
        logger.debug("request_api: {} {}", addr, url);

        boolean isLogin = userId > 0;
        boolean isOpen = openUrlList.contains(url);
//
//        if (!isOpen && !isLogin) {
//            // 401
//            writeJson(response, HttpStatus.UNAUTHORIZED.value(), "{\"message\": \"please login first\"}");
//            return false;
//        }
        request.setAttribute(StockConsts.KEY_AUTH_USER_ID, userId);
        return true;
    }

    private Long getUserId(HttpServletRequest request) {
        String authToken = request.getHeader(StockConsts.KEY_AUTH_TOKEN);
        if (StringUtils.isNotEmpty(authToken)) {
            User user = userService.getByToken(authToken);
            if (user != null) {
                return user.getId();
            }
        }
        return 0L;
    }

    private void writeJson(HttpServletResponse response, int status, String data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(data);
    }

}
