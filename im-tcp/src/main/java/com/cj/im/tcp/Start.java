package com.cj.im.tcp;


import com.cj.codec.config.BootstrapConfig;
import com.cj.im.tcp.consumer.MqMessageConsumer;
import com.cj.im.tcp.redis.RedisManage;
import com.cj.im.tcp.register.RegistryZK;
import com.cj.im.tcp.register.ZKit;
import com.cj.im.tcp.server.LimServer;
import com.cj.im.tcp.server.LimWebSocketServer;
import com.cj.im.tcp.util.MqFactory;
import com.sun.org.apache.bcel.internal.util.ClassPath;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Start {
    public static void main(String[] args) throws Exception {

        File directory = new File("im-tcp/src/main/resources");

        String reportPath = directory.getCanonicalPath();
        start(reportPath+"\\config.yml");
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

    public static void registerZk(BootstrapConfig bootstrapConfig) throws UnknownHostException {
        String ip = InetAddress.getLocalHost().getHostAddress();
        ZkClient zkClient = new ZkClient(bootstrapConfig.getIm().getZkConfig().getZkAddr(),
                bootstrapConfig.getIm().getZkConfig().getZkConnectTimeOut());
        ZKit zKit = new ZKit(zkClient);
        RegistryZK registryZK = new RegistryZK(zKit, ip, bootstrapConfig.getIm());
        new Thread(registryZK).start();
    }
}
