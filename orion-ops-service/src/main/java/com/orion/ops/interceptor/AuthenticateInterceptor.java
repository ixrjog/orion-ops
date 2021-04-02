package com.orion.ops.interceptor;

import com.alibaba.fastjson.JSON;
import com.orion.constant.StandardContentType;
import com.orion.lang.wrapper.HttpWrapper;
import com.orion.ops.annotation.IgnoreAuth;
import com.orion.ops.consts.KeyConst;
import com.orion.ops.consts.ResultCode;
import com.orion.ops.consts.UserHolder;
import com.orion.ops.entity.dto.UserDTO;
import com.orion.ops.utils.Currents;
import com.orion.servlet.web.Servlets;
import com.orion.utils.Strings;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 认证过滤器
 *
 * @author Jiahang Li
 * @version 1.0.0
 * @since 2021/4/1 17:20
 */
@Component
@Order(10)
public class AuthenticateInterceptor implements HandlerInterceptor {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod)) {
            return false;
        }
        boolean ignore = ((HandlerMethod) handler).hasMethodAnnotation(IgnoreAuth.class);
        boolean pass = false;
        String loginToken = Currents.getLoginToken(request);
        if (!Strings.isEmpty(loginToken)) {
            String cache = redisTemplate.opsForValue().get(Strings.format(KeyConst.LOGIN_TOKEN_KEY, loginToken));
            if (!Strings.isEmpty(cache)) {
                pass = true;
                UserHolder.set(JSON.parseObject(cache, UserDTO.class));
            }
        }
        if (!ignore && !pass) {
            response.setContentType(StandardContentType.APPLICATION_JSON);
            Servlets.transfer(response, HttpWrapper.of(ResultCode.UNAUTHORIZED).toJsonString().getBytes());
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.remove();
    }

}
