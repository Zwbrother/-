package com.zjsu.scholarship.security;

import com.zjsu.scholarship.common.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) return true;
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或令牌缺失");
        }
        String token = header.substring(7);
        try {
            Claims claims = jwtUtil.parse(token);
            AuthContext.CurrentUser user = new AuthContext.CurrentUser(
                    claims.get("uid", Long.class),
                    claims.get("account", String.class),
                    claims.get("role", String.class),
                    claims.get("name", String.class)
            );
            AuthContext.set(user);

            HandlerMethod hm = (HandlerMethod) handler;
            RequireRole reqRole = hm.getMethodAnnotation(RequireRole.class);
            if (reqRole == null) {
                reqRole = hm.getBeanType().getAnnotation(RequireRole.class);
            }
            if (reqRole != null) {
                boolean ok = false;
                for (String r : reqRole.value()) {
                    if (r.equals(user.role)) { ok = true; break; }
                }
                if (!ok) throw new BusinessException(403, "无权限访问该资源");
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(401, "令牌无效或已过期");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
