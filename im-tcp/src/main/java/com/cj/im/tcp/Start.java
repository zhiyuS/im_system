package com.cj.im.tcp;


import com.cj.codec.config.BootstrapConfig;
import com.cj.im.tcp.redis.RedisManage;
import com.cj.im.tcp.server.LimServer;
import com.cj.im.tcp.server.LimWebSocketServer;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

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
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(500);
        }
    }
}
