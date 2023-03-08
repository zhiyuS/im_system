package com.cj.im.tcp.server;

import com.cj.codec.config.BootstrapConfig;
import com.cj.im.tcp.handler.DiscardServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;

public class LimWebSocketServer {

    private BootstrapConfig.TcpConfig config;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap server;

    public LimWebSocketServer(BootstrapConfig.TcpConfig config) {
        this.config = config;
        this.config = config;
        bossGroup = new NioEventLoopGroup(config.getBossThreadSize()); // (1)
        workerGroup = new NioEventLoopGroup(config.getWorkThreadSize());
        server = new ServerBootstrap(); // (2)
        server.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class) // (3)
                .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("http-codec", new HttpServerCodec());
                    // 对写大数据流的支持
                    pipeline.addLast("http-chunked", new ChunkedWriteHandler());
                    // 几乎在netty中的编程，都会使用到此hanler
                    pipeline.addLast("aggregator", new HttpObjectAggregator(65535));
                    /**
                     * websocket 服务器处理的协议，用于指定给客户端连接访问的路由 : /ws
                     * 本handler会帮你处理一些繁重的复杂的事
                     * 会帮你处理握手动作： handshaking（close, ping, pong） ping + pong = 心跳
                     * 对于websocket来讲，都是以frames进行传输的，不同的数据类型对应的frames也不同
                     */
                    pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)
    }

    public void start(){
        server.bind(config.getWebSocketPort()); // (7)
        System.out.println("WebSocketServer start success");
    }
}
