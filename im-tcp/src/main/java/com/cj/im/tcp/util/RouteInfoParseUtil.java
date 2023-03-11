package com.cj.im.tcp.util;

import com.cj.im.common.BaseErrorCode;
import com.cj.im.common.exception.ApplicationException;
import com.cj.im.common.route.RouteInfo;

public class RouteInfoParseUtil {
    public static RouteInfo parse(String info){
        try {
            String[] serverInfo = info.split(":");
            RouteInfo routeInfo =  new RouteInfo(serverInfo[0], Integer.parseInt(serverInfo[1])) ;
            return routeInfo ;
        }catch (Exception e){
            throw new ApplicationException(BaseErrorCode.PARAMETER_ERROR) ;
        }
    }
}
