package com.cj.im.tcp.server;


import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.cj.codec.MessageDecoder;
import com.cj.codec.MessageEncoder;
import com.cj.codec.config.BootstrapConfig;
import com.cj.im.tcp.handler.DiscardServerHandler;
import com.cj.im.tcp.handler.HeartBertServerHandler;
import com.cj.im.tcp.handler.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LimServer {
    private BootstrapConfig.TcpConfig config;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap server;
    private static Logger logger = LoggerFactory.getLogger(LimServer.class);

    public LimServer(BootstrapConfig.TcpConfig config) {
        this.config = config;
        bossGroup = new NioEventLoopGroup(config.getBossThreadSize()); // (1)
        workerGroup = new NioEventLoopGroup(config.getWorkThreadSize());
        server = new ServerBootstrap(); // (2)
        server.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class) // (3)
                .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new MessageDecoder());
                        ch.pipeline().addLast(new MessageEncoder());

                        ch.pipeline().addLast(new IdleStateHandler(0,0,1));
                        ch.pipeline().addLast(new HeartBertServerHandler(config.getHearBertTimeOut()));

                        ch.pipeline().addLast(new NettyServerHandler(config.getBrokerId(),config.getLogicUrl()));

                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

    }

    public void start(){
        // Bind and start to accept incoming connections.
        server.bind(config.getTcpPort()); // (7)
        logger.info("tcp start success");
        System.out.println("tcp start success");


    }
}
