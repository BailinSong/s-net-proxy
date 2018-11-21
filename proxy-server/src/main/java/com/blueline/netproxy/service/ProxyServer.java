package com.blueline.netproxy.service;

import com.alibaba.fastjson.JSON;
import com.blueline.netproxy.mode.ProxyData;
import com.blueline.netproxy.mode.ProxyDataType;
import com.blueline.netproxy.mode.RuleMapping;
import com.blueline.netproxy.net.handle.IdleHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Baili
 */

@Service
@EnableAutoConfiguration
public class ProxyServer implements IProxyServer {

    private Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    private static Map<String, SocketChannel> userSocketChannel = new ConcurrentHashMap<>(10);
    private static EventLoopGroup boss = new NioEventLoopGroup();
    private static EventLoopGroup worker = new NioEventLoopGroup();
    private ServerBootstrap bootstrap = new ServerBootstrap();

    @Autowired
    RuleService ruleService;


    @Value("${proxy.port:7207}")
    int proxyPort;



    @Override
    public void disconnect(String user) {
        try {
            logger.info("Disconnect user " + user);

            SocketChannel channel = userSocketChannel.get(user);
            if (channel != null) {
                channel.close().sync();
            }

        } catch (InterruptedException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    @PostConstruct
    public void start() {

        bootstrap.group(boss, worker);

        //设置socket工厂
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel socketChannel) {
                ChannelPipeline channelPipeline = socketChannel.pipeline();
//                channelPipeline.addLast(new LoggingHandler(ProxyServer.class, LogLevel.WARN));

                channelPipeline.addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 13, 4, 0, 0));
                channelPipeline.addLast(new ByteArrayDecoder());
                channelPipeline.addLast(new ByteArrayEncoder());
                channelPipeline.addLast(new IdleStateHandler(5, 0, 0));
                channelPipeline.addLast(new IdleHandler() {
                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                        //关闭通道
                        ctx.channel().close();
                        //打印异常
                        logger.warn("Heartbeat check failed", cause);
                    }
                });
                channelPipeline.addLast(getProxyClientChannelInboundHandler());

            }

            private SimpleChannelInboundHandler<byte[]> getProxyClientChannelInboundHandler() {
                return new SimpleChannelInboundHandler<byte[]>() {


                    Map<String, List<ProxyEndPoint>> proxyEndPointListByUser = new ConcurrentHashMap<>(10);
                    Map<String, String> userMap = new ConcurrentHashMap<>(10);

                    @Override
                    public void channelActive(ChannelHandlerContext channelHandlerContext) {
                        logger.info("ProxyServer.channelActive " + channelHandlerContext.channel().remoteAddress());
                    }

                    //客户端断开
                    @Override
                    public void channelInactive(ChannelHandlerContext channelHandlerContext) {
                        List<ProxyEndPoint> proxyEndPointList = proxyEndPointListByUser.remove(channelHandlerContext.channel().remoteAddress().toString());
                        if (proxyEndPointList != null) {
                            proxyEndPointList.forEach(p -> {
                                logger.info("ProxyServer.closeProxyEndPoint " + p.proxyPort);

                                if (p.protocolType != 1) {
                                    p.close();
                                }

                            });
                        }

                        String user = userMap.remove(channelHandlerContext.channel().remoteAddress().toString());
                        userSocketChannel.remove(user);

                        logger.info("ProxyServer.channelInactive " + channelHandlerContext.channel().remoteAddress());
                    }

                    @Override
                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes) {

                        ProxyData proxyData = ProxyData.build(bytes);
                        switch (proxyData.getType()) {
                            case ProxyDataType
                                    .PROXY_DATA:
                                try {
                                    ProxyEndPoint.writeAndFlush(proxyData.getRemoteAddress(), proxyData.getData());
                                } catch (Exception e) {
                                    logger.debug("Target connection does not exist ", e);
                                }
                                break;
                            case ProxyDataType.LOGIN:


                                Map<String, String> user = JSON.parseObject(proxyData.getData(), HashMap.class);
                                ConcurrentHashMap<Integer, RuleMapping> rules = ruleService.getProxyInfo(user.get("user"), "tcp");

                                startTcpProxyEndPoint(channelHandlerContext, rules);

                                ConcurrentHashMap<Integer, RuleMapping> httpRules = ruleService.getProxyInfo(user.get("user"), "http");

                                startHttpProxyEndPoint(channelHandlerContext, httpRules);

                                rules.putAll(httpRules);

                                ProxyData data = ProxyData.build(ProxyDataType.RULES_DATA, Integer.MIN_VALUE, (InetSocketAddress) channelHandlerContext.channel().remoteAddress(), JSON.toJSONBytes(rules));

                                channelHandlerContext.writeAndFlush(data.getBytes());

                                userSocketChannel.put(user.get("user"), (SocketChannel) channelHandlerContext.channel());
                                userMap.put(channelHandlerContext.channel().remoteAddress().toString(), user.get("user"));

                                break;
                            case ProxyDataType.ENDPOINT_CLOSE:
                                ProxyEndPoint.close(proxyData.getRemoteAddress());
                                break;
                            case ProxyDataType.PING:
                                ProxyData keepAlive = ProxyData.build(ProxyDataType.PONG, Integer.MIN_VALUE, (InetSocketAddress) channelHandlerContext.channel().localAddress(), JSON.toJSONBytes(System.currentTimeMillis()));
                                channelHandlerContext.writeAndFlush(keepAlive.getBytes());
                                logger.debug("ProxyServer.keepAlive >>{}", proxyData.toString());
                                break;
                            case ProxyDataType.PONG:
                                logger.debug("ProxyServer.keepAlive <<{}", proxyData.toString());
                                break;
                            default:
                                logger.warn("ProxyServer.default" + proxyData);
                        }

                    }

                    private void startHttpProxyEndPoint(ChannelHandlerContext channelHandlerContext, ConcurrentHashMap<Integer, RuleMapping> httpRules) {
                        httpRules.forEach((k, v) -> {

                            InetSocketAddress endPointAddress = new InetSocketAddress(v.getHost(), v.getPort());

                            synchronized (HttpProxyEndPoint.httpProxyEndPointMap) {
                                HttpProxyEndPoint proxyEndPoint = (HttpProxyEndPoint) HttpProxyEndPoint.httpProxyEndPointMap.get(endPointAddress);
                                if (proxyEndPoint == null) {
                                    try {

                                        proxyEndPoint = new HttpProxyEndPoint(endPointAddress);
                                        proxyEndPoint.start();
                                        HttpProxyEndPoint.httpProxyEndPointMap.put(endPointAddress, proxyEndPoint);
                                        proxyEndPoint.put(v.getId(), (SocketChannel) channelHandlerContext.channel());
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    proxyEndPoint.put(v.getId(), (SocketChannel) channelHandlerContext.channel());
                                }

                            }
                        });
                    }

                    private void startTcpProxyEndPoint(ChannelHandlerContext channelHandlerContext, ConcurrentHashMap<Integer, RuleMapping> rules) {
                        List<ProxyEndPoint> proxyEndPointList = new LinkedList<>();

                        try {

                            rules.forEach((k, v) -> {

                                InetSocketAddress endPointAddress = new InetSocketAddress(v.getHost(), v.getPort());
                                ProxyEndPoint proxyEndPoint = new ProxyEndPoint(endPointAddress, (SocketChannel) channelHandlerContext.channel());
                                proxyEndPoint.start();
                                proxyEndPointList.add(proxyEndPoint);
                                proxyEndPointListByUser.put(channelHandlerContext.channel().remoteAddress().toString(), proxyEndPointList);


                            });
                        } catch (Exception e) {
                            logger.warn("", e);
                        }
                    }

                    //异常
                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                        //关闭通道
                        ctx.channel().close();
                        //打印异常
                        logger.warn("Unknown exception", cause);
                    }

                };
            }
        });

        bootstrap.option(ChannelOption.SO_BACKLOG, 64);
        try {
            bootstrap.bind(proxyPort).sync();
            logger.info("ProxyServer.start ... " + proxyPort);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
