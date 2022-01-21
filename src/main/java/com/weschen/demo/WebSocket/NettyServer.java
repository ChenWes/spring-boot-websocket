package com.weschen.demo.WebSocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * 服务器启动类
 */
public class NettyServer {

    /**
     * Netty服务器端口
     */
    private final int port;

    /**
     * Netty服务器
     *
     * @param port
     */
    public NettyServer(int port) {
        this.port = port;
    }

    /**
     * 开始服务
     *
     * @throws Exception
     */
    public void start() throws Exception {

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup group = new NioEventLoopGroup();

        try {

            //Netty作为服务端，从ServerBootstrap启动
            ServerBootstrap sb = new ServerBootstrap();

            //ChannelOption.SO_BACKLOG对应的是tcp/ip协议
            sb.option(ChannelOption.SO_BACKLOG, 1024);

            sb.group(group, bossGroup) // 绑定线程池
                    .channel(NioServerSocketChannel.class) // 指定使用的channel
                    .localAddress(this.port)// 绑定监听端口
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        // 初始化类

                        // 绑定客户端连接时候触发操作
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {

                            System.out.println("收到新连接：" + ch.id());

                            // websocket协议本身是基于http协议的，所以这边也要使用http解编码器
                            ch.pipeline().addLast(new HttpServerCodec());


                            // 以块的方式来写的处理器
                            ch.pipeline().addLast(new ChunkedWriteHandler());
                            ch.pipeline().addLast(new HttpObjectAggregator(8192));

                            //编码和解码写出一样的格式
                            ch.pipeline().addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
                            ch.pipeline().addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));

                            // WebSocketHandler是自己编写的代码，说明如何处理Websocket的帮助类
                            // 频道上线与下线，频道发送消息都由它处理
                            ch.pipeline().addLast(new WebSocketHandler());

                            // WebSocket协议处理器
                            ch.pipeline().addLast(new WebSocketServerProtocolHandler("/ws", null, true, 65536 * 100));


                        }


                    });


            // 服务器异步创建绑定，ServerBootstrap的核心操作是bind()方法
            ChannelFuture cf = sb.bind().sync();

            System.out.println(NettyServer.class + " 启动正在监听： " + cf.channel().localAddress());

            // 关闭服务器通道
            cf.channel().closeFuture().sync();

        } finally {

            // 释放线程池资源
            group.shutdownGracefully().sync();
            bossGroup.shutdownGracefully().sync();

        }
    }

}
