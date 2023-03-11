package com.cj.im.common.route.algorithm.random;

import com.cj.im.common.enums.UserErrorCode;
import com.cj.im.common.exception.ApplicationException;
import com.cj.im.common.route.RouteHandler;
import org.omg.PortableInterceptor.USER_EXCEPTION;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomHandler implements RouteHandler {

    @Override
    public String routeServer(List<String> values, String key) {
        int size = values.size();
        if(size <=0 ){
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }
        int i = ThreadLocalRandom.current().nextInt(size);
        return values.get(i);
    }
}
