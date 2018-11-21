package com.blueline.netproxy.net.handle;

import com.alibaba.fastjson.JSON;
import com.blueline.netproxy.mode.ProxyData;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.net.InetSocketAddress;

@ChannelHandler.Sharable
public class IdleHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                ProxyData keepAlive = ProxyData.build((byte)3,Integer.MIN_VALUE,(InetSocketAddress) ctx.channel().localAddress(), JSON.toJSONBytes(System.currentTimeMillis()));
                ctx.writeAndFlush(keepAlive.getBytes());
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
