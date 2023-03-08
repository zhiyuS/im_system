package com.cj.im.tcp.register;

import com.cj.codec.config.BootstrapConfig;
import com.cj.im.common.constant.Constants;

public class RegistryZK implements Runnable{
    private ZKit zKit;
    private String ip;
    private BootstrapConfig.TcpConfig tcpConfig;

    public RegistryZK(ZKit zKit, String ip, BootstrapConfig.TcpConfig tcpConfig) {
        this.zKit = zKit;
        this.ip = ip;
        this.tcpConfig = tcpConfig;
    }

    @Override
    public void run() {
        zKit.createRootNode();
        String tcpPath = Constants.ImCoreZkRoot + Constants.ImCoreZkRootTcp + "/" + ip + ":" + tcpConfig.getTcpPort();

        zKit.createNode(tcpPath);
        System.out.println(tcpPath);
        String webPath = Constants.ImCoreZkRoot + Constants.ImCoreZkRootWeb + "/" + ip + ":" + tcpConfig.getWebSocketPort();
        zKit.createNode(webPath);
        System.out.println(webPath);
    }
}
