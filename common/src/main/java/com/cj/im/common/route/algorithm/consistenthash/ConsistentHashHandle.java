package com.cj.im.common.route.algorithm.consistenthash;

import com.cj.im.common.route.RouteHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 一致性哈希算法
 */
public class ConsistentHashHandle implements RouteHandler {

    private AbstractConsistentHash hash;
    public void setHash(AbstractConsistentHash hash) {
        this.hash = hash;
    }

    @Override
    public String routeServer(List<String> values, String key) {
        return hash.process(values,key);
    }
}
