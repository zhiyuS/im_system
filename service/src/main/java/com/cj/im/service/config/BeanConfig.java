package com.cj.im.service.config;

import com.cj.im.common.config.AppConfig;
import com.cj.im.common.enums.ImUrlRouteWayEnum;
import com.cj.im.common.enums.RouteHashMethodEnum;
import com.cj.im.common.route.RouteHandler;
import com.cj.im.common.route.algorithm.consistenthash.AbstractConsistentHash;
import com.cj.im.common.route.algorithm.consistenthash.ConsistentHashHandle;
import com.cj.im.common.route.algorithm.consistenthash.TreeMapConsistentHash;
import com.cj.im.common.route.algorithm.loop.LoopHandler;
import com.cj.im.common.route.algorithm.random.RandomHandler;
import com.cj.im.service.utils.SnowflakeIdWorker;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Configuration
public class BeanConfig {
    @Autowired
    private AppConfig appConfig;

    /**
     * 通过反射调用负载均衡算法
     * @return
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    @Bean
    public RouteHandler routeHandler() throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        Integer imRouteWay = appConfig.getImRouteWay();
        String routeWay = "";
        ImUrlRouteWayEnum handler = ImUrlRouteWayEnum.getHandler(imRouteWay);
        routeWay = handler.getClazz();

        RouteHandler routeHandler = (RouteHandler)Class.forName(routeWay).newInstance();

        if(handler == ImUrlRouteWayEnum.HASH){
            //获取set方法
            Method setHash = Class.forName(routeWay).getMethod("setHash", AbstractConsistentHash.class);

            //获取哈希实现的类
            Integer consistentHashWay = appConfig.getConsistentHashWay();
            RouteHashMethodEnum handler1 = RouteHashMethodEnum.getHandler(consistentHashWay);
            String hashWay = "";
            hashWay = handler1.getClazz();
            AbstractConsistentHash consistentHash = (AbstractConsistentHash)Class.forName(hashWay).newInstance();

            setHash.invoke(routeHandler,consistentHash);

        }

        return routeHandler;
    }
    @Bean
    public ZkClient buildZkClient(){

        return new ZkClient(appConfig.getZkAddr(),appConfig.getZkConnectTimeOut());
    }

    @Bean
    public EasySqlInjector easySqlInjector () {
        return new EasySqlInjector();
    }

    @Bean
    public SnowflakeIdWorker buildSnowflakeSeq() throws Exception {
        return new SnowflakeIdWorker(0);
    }

}
