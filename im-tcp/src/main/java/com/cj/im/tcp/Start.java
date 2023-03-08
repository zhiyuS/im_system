package com.cj.im.tcp;


import com.cj.codec.config.BootstrapConfig;
import com.cj.im.tcp.consumer.MqMessageConsumer;
import com.cj.im.tcp.redis.RedisManage;
import com.cj.im.tcp.register.RegistryZK;
import com.cj.im.tcp.register.ZKit;
import com.cj.im.tcp.server.LimServer;
import com.cj.im.tcp.server.LimWebSocketServer;
import com.cj.im.tcp.util.MqFactory;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Start {
    public static void main(String[] args) throws Exception {
        if(args.length>=1){
            start(args[0]);
        }
    }
    public static void start(String filepath){
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(filepath);
            Yaml yaml = new Yaml();
            BootstrapConfig bootstrapConfig = yaml.loadAs(fileInputStream, BootstrapConfig.class);
            new LimServer(bootstrapConfig.getIm()).start();
            new LimWebSocketServer(bootstrapConfig.getIm()).start();

            RedisManage.init(bootstrapConfig);
            MqFactory.init(bootstrapConfig.getIm().getRabbitmq());
            MqMessageConsumer.init(bootstrapConfig.getIm().getBrokerId().toString());
            registerZk(bootstrapConfig);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(500);
        }
    }
//    Manage ZooKeeper in IntelliJ IDEA. After install org.apache.zookeeper.ZooKeeper plugin,
//    please open "Preferences" to set connection information in "ZooKeeper" item,
//    then you will find a "ZooKeeper" tool windown, click "ZooKeeper" tool window to visit ZK file system.
//    Double click the leaf to edit node value
//    Right click editor to update node value into ZooKeeper
//    Right click ZK tree to execute "edit", "add" and "delete" operation
//    Recursive support for add and delete operation
//    Node filter support
//    Click "Refresh" button on "ZooKeeper" tool window to refress ZK tree or node
//    Icon for different type, and transparent icon for ephemeral node
//    Stat tooltip, Copy node path
    public static void registerZk(BootstrapConfig bootstrapConfig) throws UnknownHostException {
        String ip = InetAddress.getLocalHost().getHostAddress();
        ZkClient zkClient = new ZkClient(bootstrapConfig.getIm().getZkConfig().getZkAddr(),
                bootstrapConfig.getIm().getZkConfig().getZkConnectTimeOut());
        ZKit zKit = new ZKit(zkClient);
        RegistryZK registryZK = new RegistryZK(zKit, ip, bootstrapConfig.getIm());
        new Thread(registryZK).start();
    }
}
