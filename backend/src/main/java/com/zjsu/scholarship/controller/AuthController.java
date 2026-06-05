package com.zjsu.scholarship.controller;

import com.zjsu.scholarship.common.R;
import com.zjsu.scholarship.security.AuthContext;
import com.zjsu.scholarship.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return R.ok(authService.login(body.get("account"), body.get("password")));
    }

    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        AuthContext.CurrentUser u = AuthContext.get();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", u.userId);
        data.put("account", u.account);
        data.put("role", u.role);
        data.put("name", u.name);
        return R.ok(data);
    }

    @PostMapping("/change-password")
    public R<Void> changePassword(@RequestBody Map<String, String> body) {
        AuthContext.CurrentUser u = AuthContext.get();
        authService.changePassword(u.userId, body.get("oldPassword"), body.get("newPassword"));
        return R.ok();
    }
}
