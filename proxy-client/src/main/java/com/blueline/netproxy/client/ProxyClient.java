package com.blueline.netproxy.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.blueline.netproxy.mode.ProxyData;
import com.blueline.netproxy.mode.ProxyDataType;
import com.blueline.netproxy.mode.RuleMapping;
import com.blueline.netproxy.net.handle.IdleHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Baili
 */
public class ProxyClient {

    private String proxyServerHost;
    private String proxyServerPort;
    private String proxyServerUser;
    private String proxyServerPwd;
    private EventLoopGroup worker = new NioEventLoopGroup();
    private ConcurrentHashMap<Integer, RuleMapping> proxyConfigMap;

    private ConcurrentHashMap<InetSocketAddress, EndPoint> endPointChannelMap = new ConcurrentHashMap<>();

    private ProxyClient(String user, String pwd, String host, String port) {
        proxyServerUser = user;
        proxyServerPwd = pwd;
        proxyServerHost = host;
        proxyServerPort = port;
    }

    private void start() throws InterruptedException {
        try {
            Bootstrap bootstrap = new Bootstrap();

            bootstrap.group(worker);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

            //设置socket工厂
            bootstrap.channel(NioSocketChannel.class);

            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) {
                    //获取管道
                    ChannelPipeline channelPipeline = socketChannel.pipeline();
//                    channelPipeline.addLast(new LoggingHandler(ProxyClient.class, LogLevel.WARN));

                    channelPipeline.addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 13, 4, ProxyDataType.PROXY_DATA, ProxyDataType.PROXY_DATA));
                    channelPipeline.addLast(new ByteArrayDecoder());
                    channelPipeline.addLast(new ByteArrayEncoder());
                    channelPipeline.addLast(new IdleStateHandler(5, ProxyDataType.PROXY_DATA, ProxyDataType.PROXY_DATA));
                    channelPipeline.addLast(new IdleHandler() {
                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

                            ctx.channel().close();
                            cause.printStackTrace();
                            quit(2);
                        }
                    });
                    channelPipeline.addLast(new SimpleChannelInboundHandler<byte[]>() {

                        @Override
                        public void channelActive(ChannelHandlerContext ctx) {

                            Map<String, String> user = new HashMap<>();
                            user.put("user", proxyServerUser);
                            user.put("passwd", proxyServerPwd);
                            ProxyData proxyData = ProxyData.build((byte) ProxyDataType.LOGIN, Integer.MIN_VALUE, (InetSocketAddress) ctx.channel().remoteAddress(), JSON.toJSONBytes(user));
                            ctx.writeAndFlush(proxyData.getBytes());
                            System.out.println("ProxyClient.channelActive " + ctx.channel().remoteAddress());
                        }


                        @Override
                        public void channelInactive(ChannelHandlerContext ctx) {
                            endPointChannelMap.forEachValue(1, EndPoint::close);
                            System.out.println("ProxyClient.channelInactive " + ctx.channel().remoteAddress());
                            quit(1);
                        }

                        @Override
                        protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes) throws Exception {

                            ProxyData proxyData = ProxyData.build(bytes);
                            EndPoint endpoint;
                            switch (proxyData.getType()) {
                                case ProxyDataType.PROXY_DATA:
                                    endpoint = endPointChannelMap.get(proxyData.getRemoteAddress());
                                    endpoint.writeAndFlush(proxyData.getData());
                                    break;
                                case ProxyDataType.RULES_DATA:
                                    updateProxyConfigMap(proxyData);
                                    break;
                                case ProxyDataType.ENDPOINT_OPEN:
                                    RuleMapping ruleMapping = proxyConfigMap.get(proxyData.getRule());
                                    InetSocketAddress endPointAddress = new InetSocketAddress(ruleMapping.getRealHost(), ruleMapping.getRealPort());
                                    endpoint = new EndPoint(endPointAddress, proxyData.getRemoteAddress(), (SocketChannel) channelHandlerContext.channel());
                                    endpoint.start();
                                    endPointChannelMap.put(proxyData.getRemoteAddress(), endpoint);
                                    break;
                                case ProxyDataType.ENDPOINT_CLOSE:
                                    endpoint = endPointChannelMap.remove(proxyData.getRemoteAddress());
                                    endpoint.close();
                                    break;
                                case ProxyDataType.PING:
                                    ProxyData keepAlive = ProxyData.build(ProxyDataType.PONG, Integer.MIN_VALUE, (InetSocketAddress) channelHandlerContext.channel().localAddress(), JSON.toJSONBytes(System.currentTimeMillis()));
                                    channelHandlerContext.writeAndFlush(keepAlive.getBytes());
                                    System.out.println("ProxyServer.keepAlive >> " + proxyData);
                                    break;
                                case ProxyDataType.PONG:
                                    System.out.println("ProxyServer.keepAlive << " + proxyData);
                                    break;
                                default:
                                    System.out.println("ProxyClient.default " + proxyData);
                            }


                        }

                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

                            ctx.channel().close();
                            cause.printStackTrace();
                            quit(2);
                        }

                    });
                }
            });


            ChannelFuture futrue = bootstrap.connect(new InetSocketAddress(proxyServerHost, Integer.valueOf(proxyServerPort))).sync();


            futrue.channel().closeFuture().sync();

        } finally {
            quit(0);
        }
    }

    private void quit(int status) {
        System.exit(status);
    }


    private void updateProxyConfigMap(ProxyData proxyData) {
        proxyConfigMap = JSON.parseObject(new String(proxyData.getData()), new TypeReference<ConcurrentHashMap<Integer, RuleMapping>>() {
        });
    }

    public static void main(String[] args) {
        ProxyClient proxyClient;
        try {

            String[] userInfo;
            String[] serverInfo;
            try {
                String[] params = args[0].split("@");
                userInfo = params[0].split(":");
                serverInfo = params[1].split(":");
                if ((userInfo.length & serverInfo.length) != 2) {
                    throw new RuntimeException("The information is not complete");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            proxyClient = new ProxyClient(userInfo[0], userInfo[1], serverInfo[0], serverInfo[1]);
            proxyClient.start();

        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            System.out.println("Incomplete connection information");
            System.out.println("USER:PWD@HOST:[PORT]");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


}
