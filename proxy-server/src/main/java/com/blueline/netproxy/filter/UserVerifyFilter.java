package com.blueline.netproxy.filter;

import com.alibaba.fastjson.JSON;
import com.blueline.netproxy.extend.RequestParameterWrapper;
import com.blueline.netproxy.modle.FailureResult;
import com.blueline.netproxy.modle.Result;
import com.blueline.netproxy.service.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Baili
 */
@Component
@WebFilter
@Order(10000)
public class UserVerifyFilter implements Filter {

    private Logger logger = LoggerFactory.getLogger(UserVerifyFilter.class);


    private final IUserService userService;

    @Value("${token.cycle:300000}")
    long tokenCycle;

    @Autowired
    public UserVerifyFilter(IUserService userService) {
        this.userService = userService;
    }

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {


        HttpServletRequest req = (HttpServletRequest) servletRequest;

        servletResponse.setCharacterEncoding("UTF-8");
        servletResponse.setContentType("application/json; charset=utf-8");
        String token = req.getHeader("token");
        String user = "";
        boolean isFilter = false;
        Result result = null;

        if (token == null || token.isEmpty()) {
            result = new FailureResult("40000", "Invalid token");
            isFilter = true;
        }

        if (!isFilter) {

            String tokenString = new String(Base64Utils.decodeFromString(token), StandardCharsets.UTF_8);
            String[] tokenInfo = tokenString.split(":");
            try {
                if (Math.abs(Long.valueOf(tokenInfo[2]) - System.currentTimeMillis()) < tokenCycle) {
                    if (!userService.verify(tokenInfo[0], tokenInfo[1])) {
                        isFilter = true;
                        result = new FailureResult("40001", "Verification failed");
                    } else {
                        user = tokenInfo[0];
                    }
                } else {
                    isFilter = true;
                    result = new FailureResult("40002", "Token expired");
                }

            } catch (Exception e) {
                result = new FailureResult("40000", "Invalid token");
                isFilter = true;
                e.printStackTrace();
            }

        }

        if (isFilter) {
            filtered(servletResponse, result, logger);
        } else {
            Map<String, Object> extraParams = new HashMap<>(1);
            extraParams.put("user", user);
            RequestParameterWrapper requestParameterWrapper = new RequestParameterWrapper((HttpServletRequest) servletRequest);
            requestParameterWrapper.addParameters(extraParams);
            filterChain.doFilter(requestParameterWrapper, servletResponse);
        }

    }

    private void filtered(ServletResponse servletResponse, Object result, Logger logger){
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(servletResponse.getOutputStream(), StandardCharsets.UTF_8), true)) {
            String jsonStr = JSON.toJSONString(result);
            writer.write(jsonStr);
            writer.flush();
        } catch (IOException e) {
            logger.error("Filter return message failed:" + e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {

    }
}
