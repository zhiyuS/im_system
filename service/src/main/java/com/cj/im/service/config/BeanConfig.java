package com.cj.im.service.config;

import com.cj.im.common.route.RouteHandler;
import com.cj.im.common.route.algorithm.random.RandomHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {
    @Bean
    public RouteHandler routeHandler(){
        return new RandomHandler();
    }
}
