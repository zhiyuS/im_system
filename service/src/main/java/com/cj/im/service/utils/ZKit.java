package com.cj.im.tcp.register;
import com.cj.im.common.constant.Constants;
import org.I0Itec.zkclient.ZkClient;
public class ZKit {
    private ZkClient zkClient;

    public ZKit(ZkClient zkClient) {
        this.zkClient = zkClient;
    }
    /**
     * 创建节点
     * im-coreRoot/tcp/ip:port
     */
    public void createRootNode(){
        boolean exists = zkClient.exists(Constants.ImCoreZkRoot);
        if(!exists){
            zkClient.createPersistent(Constants.ImCoreZkRoot);
        }
        boolean existsTcp = zkClient.exists(Constants.ImCoreZkRoot+Constants.ImCoreZkRootTcp);
        if(!existsTcp){
            zkClient.createPersistent(Constants.ImCoreZkRoot + Constants.ImCoreZkRootTcp);
        }
        boolean existsWeb = zkClient.exists(Constants.ImCoreZkRoot +Constants.ImCoreZkRootWeb);
        if(!existsTcp){
            zkClient.createPersistent(Constants.ImCoreZkRoot +Constants.ImCoreZkRootWeb);
        }


    }
    //ip+port 生成节点
    public void createNode(String path){
        if(!zkClient.exists(path)){
            zkClient.createPersistent(path);
        }
    }
}
