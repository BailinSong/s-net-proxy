package com.blueline.netproxy.service;

import com.blueline.netproxy.checker.IpChecker;
import com.blueline.netproxy.mode.ProxyData;
import com.blueline.netproxy.mode.ProxyDataType;
import com.blueline.netproxy.mode.RuleMapping;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Baili
 */
public class ProxyEndPoint {
    static final Logger logger = LoggerFactory.getLogger(ProxyEndPoint.class);
    static EventLoopGroup boss = new NioEventLoopGroup();
    static EventLoopGroup worker = new NioEventLoopGroup();
    static ConcurrentHashMap<InetSocketAddress, ChannelHandlerContext> remoteChannleMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<InetSocketAddress, Integer> ruleIdMap = new ConcurrentHashMap<>();
    ServerBootstrap bootstrap = new ServerBootstrap();
    InetSocketAddress proxyPort;
    SocketChannel proxyClientSocketChannel;
    ChannelFuture future;
    int protocolType = 0;

    RuleService ruleService = SpringComponent.getBean(RuleService.class);


    public ProxyEndPoint(InetSocketAddress proxyPort, SocketChannel ProxyClientSocketChannel) {
        this.proxyPort = proxyPort;
        this.proxyClientSocketChannel = ProxyClientSocketChannel;
    }

    public static void close(InetSocketAddress remoteAddress) {
        try {
            ChannelHandlerContext ep = remoteChannleMap.get(remoteAddress);
            if (ep != null) {
                ep.close();
            }

        } catch (Exception e) {
            synchronized (System.err) {
                logger.info("ProxyEndPoint.channelClose " + remoteAddress, e);
            }

        }
    }

    public static void writeAndFlush(InetSocketAddress remoteAddress, Object object) {
        remoteChannleMap.get(remoteAddress).writeAndFlush(object);
    }

    public void close() {
        future.channel().close();
    }

    public ChannelInitializer<SocketChannel> getChannelInitializer() {

        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                ChannelPipeline channelPipeline = socketChannel.pipeline();
                channelPipeline.addLast(new ByteArrayDecoder());
                channelPipeline.addLast(new ByteArrayEncoder());
                channelPipeline.addLast(new SimpleChannelInboundHandler<byte[]>() {

                    @Override
                    public void channelActive(ChannelHandlerContext channelHandlerContext) {
                        InetSocketAddress addr = (InetSocketAddress)channelHandlerContext.channel().remoteAddress();

                        Integer id = ruleIdMap.get(addr);

                        if (id == null) {
                            RuleMapping rule = ruleService.getRule(channelHandlerContext.channel().localAddress());



                            //todo 校验规则白名单
                            if(rule.getWhiteList().size()>0) {
                                if (IpChecker.judgeOr(addr.getAddress().getHostAddress(), rule.getWhiteList())) {
                                    throw new RuntimeException("reject rule[" + rule.getId() + "]:" + addr.getAddress().getHostAddress());
                                }
                            }

                            ruleIdMap.put(addr, rule.getId());
                            id = rule.getId();
                        }
                        ProxyData proxyData = ProxyData.build(ProxyDataType.ENDPOINT_OPEN, id, addr, new byte[0]);
                        remoteChannleMap.put(proxyData.getRemoteAddress(), channelHandlerContext);
                        proxyClientSocketChannel.writeAndFlush(proxyData.getBytes());
                        logger.debug("ProxyEndPoint.channelActive " + channelHandlerContext.channel().remoteAddress());
                    }

                    //客户端断开
                    @Override
                    public void channelInactive(ChannelHandlerContext channelHandlerContext) {

                        SocketAddress addr = channelHandlerContext.channel().remoteAddress();

                        Integer id = ruleIdMap.get(addr);

                        ProxyData proxyData = ProxyData.build(ProxyDataType.ENDPOINT_CLOSE, id, (InetSocketAddress) addr, new byte[0]);
                        remoteChannleMap.remove(proxyData.getRemoteAddress());
                        proxyClientSocketChannel.writeAndFlush(proxyData.getBytes());
                        logger.debug("ProxyEndPoint.channelInactive " + channelHandlerContext.channel().remoteAddress());
                    }

                    @Override
                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes) {
                        SocketAddress addr = channelHandlerContext.channel().remoteAddress();

                        Integer id = ruleIdMap.get(addr);

                        ProxyData proxyData = ProxyData.build(ProxyDataType.PROXY_DATA, id, (InetSocketAddress) channelHandlerContext.channel().remoteAddress(), bytes);
//                        try {
//                            System.out.println(new String(bytes));
//                        } catch (Exception e) {
//                        }
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

        };
    }

    public void start() {

        bootstrap.group(boss, worker);

        //设置socket工厂
        bootstrap.channel(NioServerSocketChannel.class);

        bootstrap.childHandler(getChannelInitializer());

        bootstrap.option(ChannelOption.SO_BACKLOG, 64);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE,true);
        try {
            future = bootstrap.bind(proxyPort).sync();
            logger.info("ProxyEndPoint.start ... " + proxyPort);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {

        }


    }
}
