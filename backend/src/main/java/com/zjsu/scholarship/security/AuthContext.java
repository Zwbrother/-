package com.zjsu.scholarship.security;

public class AuthContext {
    private static final ThreadLocal<CurrentUser> CTX = new ThreadLocal<>();

    public static void set(CurrentUser user) { CTX.set(user); }
    public static CurrentUser get() { return CTX.get(); }
    public static void clear() { CTX.remove(); }

    public static class CurrentUser {
        public Long userId;
        public String account;
        public String role;
        public String name;
        public CurrentUser(Long userId, String account, String role, String name) {
            this.userId = userId; this.account = account; this.role = role; this.name = name;
        }
    }
}
