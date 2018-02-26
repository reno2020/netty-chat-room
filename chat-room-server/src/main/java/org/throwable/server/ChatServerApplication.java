package org.throwable.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.throwable.server.handler.ChatRoomServerInitializer;

/**
 * @author throwable
 * @version v1.0
 * @description
 * @since 2018/2/26 10:13
 */
@Slf4j
@SpringBootApplication
public class ChatServerApplication implements CommandLineRunner {

    private static final int DEFAULT_PORT = 9100;

    @Autowired
    private Environment environment;

    public static void main(String[] args) {
        SpringApplication.run(ChatServerApplication.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
        int port = environment.getProperty("server.port", Integer.class, DEFAULT_PORT) + 1;
        //boss用来接收进来的连接
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        //worker用来处理已经被接收的连接
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChatRoomServerInitializer())
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = bootstrap.bind("127.0.0.1", port).sync();
            log.info(String.format("Chat room server starts successfully,port : %d", port));
            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
