package com.cj.im.common.route.algorithm.loop;

import com.cj.im.common.enums.UserErrorCode;
import com.cj.im.common.exception.ApplicationException;
import com.cj.im.common.route.RouteHandler;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class LoopHandler implements RouteHandler {
    private volatile AtomicInteger index = new AtomicInteger();
    @Override
    public String routeServer(List<String> values, String key) {
        int size = values.size();
        if(size <=0 ){
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }
        int i = index.incrementAndGet() % size;
        return values.get(i);
    }
}
