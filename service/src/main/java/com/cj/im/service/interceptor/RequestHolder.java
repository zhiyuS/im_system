package com.cj.im.service.interceptor;

public class RequestHolder {
    private static final ThreadLocal<Boolean> requestHolder = new ThreadLocal<>();

    public static void set(Boolean isAdmin){requestHolder.set(isAdmin);}
    public static Boolean get() {
        return requestHolder.get();
    }
    public static void remove(){
        requestHolder.remove();
    }
}
