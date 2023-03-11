package com.cj.im.service.utils;
import com.cj.im.common.constant.Constants;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Component
public class ZKit {

    @Autowired
    private ZkClient zkClient;

    public ZKit(ZkClient zkClient) {
        this.zkClient = zkClient;
    }
    /**
     * 创建节点
     * im-coreRoot/tcp/ip:port
     */
    public List<String> getAllTcpNode(){
        return zkClient.getChildren(Constants.ImCoreZkRoot+Constants.ImCoreZkRootTcp);
    }
    //ip+port 生成节点
    public List<String> getAllWebNode(){
        return zkClient.getChildren(Constants.ImCoreZkRoot+Constants.ImCoreZkRootWeb);
    }
}
