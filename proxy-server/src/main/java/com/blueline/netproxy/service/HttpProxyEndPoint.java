package com.blueline.netproxy.service;

import com.blueline.netproxy.checker.IpChecker;
import com.blueline.netproxy.filter.UserVerifyFilter;
import com.blueline.netproxy.mode.ProxyData;
import com.blueline.netproxy.mode.ProxyDataType;
import com.blueline.netproxy.mode.RuleMapping;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.util.ByteProcessor;
import io.netty.util.internal.AppendableCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Baili
 */
public class HttpProxyEndPoint extends ProxyEndPoint {
    private Logger logger = LoggerFactory.getLogger(HttpProxyEndPoint.class);
    final static ConcurrentHashMap<InetSocketAddress, ProxyEndPoint> httpProxyEndPointMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, SocketChannel> ruleMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<InetSocketAddress, SocketChannel> clientChannleMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<InetSocketAddress, Integer> ruleIdMap = new ConcurrentHashMap<>();

    private RuleService ruleService = SpringComponent.getBean(RuleService.class);

    HttpProxyEndPoint(InetSocketAddress proxyPort) {
        super(proxyPort, null);
        protocolType = 1;
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {

        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                ChannelPipeline channelPipeline = socketChannel.pipeline();
//                channelPipeline.addLast(new LoggingHandler(HttpProxyEndPoint.class, LogLevel.DEBUG));
                channelPipeline.addLast(new ByteToMessageDecoder() {
                    AppendableCharSequence seq = new AppendableCharSequence(2048);
                    HeaderParser headerParser = new HeaderParser(seq, 2048);

                    @Override
                    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {

                        AppendableCharSequence appendableCharSequence = headerParser.parse(byteBuf);
                        if (appendableCharSequence != null) {
                            list.add(appendableCharSequence.toString());
                        }

                        byte[] array = new byte[byteBuf.readableBytes()];
                        byteBuf.readBytes(array);
                        list.add(array);
                    }
                });
                channelPipeline.addLast(new ByteArrayEncoder());
                channelPipeline.addLast(new SimpleChannelInboundHandler<Object>() {

                    @Override
                    public void channelActive(ChannelHandlerContext channelHandlerContext) {


                        logger.debug("ProxyEndPoint.channelActive " + channelHandlerContext.channel().remoteAddress());
                    }

                    //客户端断开
                    @Override
                    public void channelInactive(ChannelHandlerContext channelHandlerContext) {
                        SocketAddress remoteAddress = channelHandlerContext.channel().remoteAddress();
                        int id = ruleIdMap.remove(remoteAddress);
                        ProxyData proxyData = ProxyData.build(ProxyDataType.ENDPOINT_CLOSE, id, (InetSocketAddress) remoteAddress, new byte[0]);
                        remoteChannleMap.remove(proxyData.getRemoteAddress());
                        clientChannleMap.remove(proxyData.getRemoteAddress());

                        SocketChannel clientChannel = clientChannleMap.get(remoteAddress);
                        if (clientChannel != null) {
                            clientChannel.writeAndFlush(proxyData.getBytes());
                        }

                        logger.debug("ProxyEndPoint.channelInactive " + channelHandlerContext.channel().remoteAddress());
                    }

                    @Override
                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object bytes) {
                        ProxyData proxyData = null;
                        InetSocketAddress addr = (InetSocketAddress)channelHandlerContext.channel().remoteAddress();
                        if (bytes instanceof String) {
                            logger.info(bytes.toString());
                            String[] httpRH = ((String) bytes).split(" ");

                            if(httpRH.length<2){
                                channelHandlerContext.channel().close();
                                logger.debug("Illegal HTTP Request header {}",bytes);
                                return;
                            }

                            RuleMapping rule = ruleService.getRule(channelHandlerContext.channel().localAddress(), httpRH[1]);

                            if(rule==null){
                                channelHandlerContext.channel().close();
                                logger.debug("No matching rules found for {}",bytes);
                                return;
                            }

                            //todo 校验规则白名单
                            if(rule.getWhiteList().size()>0) {
                                if (IpChecker.judgeOr(addr.getAddress().getHostAddress(), rule.getWhiteList())) {
                                    throw new RuntimeException("reject rule[" + rule.getId() + "]:" + addr.getAddress().getHostAddress());
                                }
                            }

                            httpRH[1] = httpRH[1].replace(rule.getPath(), rule.getRealPath());
//                            if(httpRH[1].startsWith("/")) httpRH[1]=httpRH[1].s
                            SocketChannel clientChannel = ruleMap.get(rule.getId());

                            ProxyData openConnectData = ProxyData.build(ProxyDataType.ENDPOINT_OPEN, rule.getId(), addr, new byte[0]);
                            remoteChannleMap.put(openConnectData.getRemoteAddress(), channelHandlerContext);
                            clientChannleMap.put(openConnectData.getRemoteAddress(), clientChannel);
                            ruleIdMap.put(openConnectData.getRemoteAddress(), rule.getId());
                            clientChannel.writeAndFlush(openConnectData.getBytes());

                            StringBuffer stringBuffer = new StringBuffer();
                            for (String aHttpRH : httpRH) {
                                stringBuffer.append(aHttpRH).append((char) 32);
                            }
                            stringBuffer.setLength(stringBuffer.length() - 1);
                            stringBuffer.append("\r\n");
                            proxyData = ProxyData.build(ProxyDataType.PROXY_DATA, rule.getId(), addr, stringBuffer.toString().getBytes());

                        }

                        if (bytes instanceof byte[]) {
                            proxyData = ProxyData.build(ProxyDataType.PROXY_DATA, ruleIdMap.get(addr), addr, (byte[]) bytes);
                        }

                        if (proxyData != null) {
                            SocketChannel clientChannel = clientChannleMap.get(proxyData.getRemoteAddress());
                            if (clientChannel != null) {
                                clientChannel.writeAndFlush(proxyData.getBytes());
                            }

                        }

                    }

                    //异常
                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                        //关闭通道
                        ctx.channel().close();
                        //打印异常
                        logger.warn("Unknown exception",cause);
                    }


                });

            }

        };
    }

    synchronized void put(int id, SocketChannel channel) {
        ruleMap.put(id, channel);
    }

    private static class HeaderParser implements ByteProcessor {
        private final AppendableCharSequence seq;
        private final int maxLength;
        private int size;
        private static final char EC='\n';
        private static final char RC='\r';

        HeaderParser(AppendableCharSequence seq, int maxLength) {
            this.seq = seq;
            this.maxLength = maxLength;
        }

        TooLongFrameException newException(int maxLength) {
            return new TooLongFrameException("HTTP header is larger than " + maxLength + " bytes.");
        }

        AppendableCharSequence parse(ByteBuf buffer) {
            int oldSize = this.size;
            this.seq.reset();
            int i = buffer.forEachByte(this);
            if (i == -1) {
                this.size = oldSize;
                return null;
            } else {
                buffer.readerIndex(i + 1);
                return this.seq;
            }
        }

        @Override
        public boolean process(byte value) {
            char nextByte = (char) (value & 255);
            if (nextByte == RC) {
                return true;
            } else {
                if (nextByte == EC) {
                    return false;
                } else if (++this.size > this.maxLength) {
                    throw this.newException(this.maxLength);
                } else {
                    this.seq.append(nextByte);
                    return true;
                }
            }
        }


        public void reset() {
            this.size = 0;
        }
    }
}
