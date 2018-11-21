package com.blueline.netproxy.client;

import com.blueline.netproxy.mode.ProxyData;
import com.blueline.netproxy.mode.ProxyDataType;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

import java.net.InetSocketAddress;

class EndPoint {

    private static EventLoopGroup worker = new NioEventLoopGroup();
    private InetSocketAddress endpoint;
    private InetSocketAddress remoter;
    private SocketChannel proxyClientSocketChannel;
    private ChannelFuture future;


    EndPoint(InetSocketAddress endpoint, InetSocketAddress remoter, SocketChannel proxyClientSocketChannel) {
        this.endpoint = endpoint;
        this.remoter = remoter;
        this.proxyClientSocketChannel = proxyClientSocketChannel;
    }

    void close() {
        future.channel().close();

    }

    void start() throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();

        bootstrap.group(worker);

        //设置socket工厂
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE,true);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                //获取管道
                ChannelPipeline channelPipeline = socketChannel.pipeline();
//                channelPipeline.addLast(new LoggingHandler(EndPoint.class, LogLevel.DEBUG));
                channelPipeline.addLast(new ByteArrayDecoder());
                channelPipeline.addLast(new ByteArrayEncoder());

                channelPipeline.addLast(new SimpleChannelInboundHandler<byte[]>() {

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) {
                        System.out.println("EndPoint.channelActive " + remoter + " >> " + ctx.channel().localAddress() + " >> " + ctx.channel().remoteAddress());
                    }

                    //客户端断开
                    @Override
                    public void channelInactive(ChannelHandlerContext channelHandlerContext) {
                        System.out.println("EndPoint.channelInactive " + remoter + " >> " + channelHandlerContext.channel().localAddress() + " >> " + channelHandlerContext.channel().remoteAddress());

                        ProxyData proxyData = ProxyData.build(ProxyDataType.ENDPOINT_CLOSE, Integer.MIN_VALUE, remoter, new byte[0]);
                        proxyClientSocketChannel.writeAndFlush(proxyData.getBytes());
                    }

                    @Override
                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes) {

                        ProxyData proxyData = ProxyData.build(ProxyDataType.PROXY_DATA, Integer.MIN_VALUE, remoter, bytes);
                        proxyClientSocketChannel.writeAndFlush(proxyData.getBytes());

                    }

                    //异常
                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                        //关闭通道
                        ctx.channel().close();
                        //打印异常
                        cause.printStackTrace();
                    }

                });
            }
        });

        //发起异步连接操作
        future = bootstrap.connect(endpoint).sync();

    }

    void writeAndFlush(Object object) {
        future.channel().writeAndFlush(object);
    }

}
