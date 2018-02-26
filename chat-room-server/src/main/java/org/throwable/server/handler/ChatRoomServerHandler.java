package org.throwable.server.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author throwable
 * @version v1.0
 * @description
 * @since 2018/2/26 10:33
 */
@Slf4j
@Component
public class ChatRoomServerHandler extends SimpleChannelInboundHandler<String> {

    private static final String SUFFIX = "$_$";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        for (Channel each : CHANNEL_GROUP) {
            each.writeAndFlush(String.format("CLIENT-[%s] enters chat room%s", channel.remoteAddress(), SUFFIX));
        }
        CHANNEL_GROUP.add(channel);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        for (Channel each : CHANNEL_GROUP) {
            each.writeAndFlush(String.format("CLIENT-[%s] leaves chat room%s", channel.remoteAddress(), SUFFIX));
        }
        CHANNEL_GROUP.remove(channel);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        log.info(String.format("Chat client [%s] is online", channel.remoteAddress()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        log.info(String.format("Chat client [%s] is offline", channel.remoteAddress()));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String message) throws Exception {
        Channel channel = ctx.channel();
        for (Channel each : CHANNEL_GROUP) {
            if (!channel.equals(each)) {
                each.writeAndFlush(String.format("%s-[%s] : %s%s", FORMATTER.format(LocalDateTime.now()), each.remoteAddress(), message, SUFFIX));
            } else {
                each.writeAndFlush(String.format("%s-[You] : %s%s", FORMATTER.format(LocalDateTime.now()), message, SUFFIX));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        log.error(String.format("Chat client [%s] encounter an exception", channel.remoteAddress()), cause);
        ctx.close();
    }
}
