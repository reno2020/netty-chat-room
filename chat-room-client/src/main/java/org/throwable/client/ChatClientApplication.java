package org.throwable.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.throwable.client.handler.ChatRoomClientHandler;

/**
 * @author throwable
 * @version v1.0
 * @description
 * @since 2018/2/26 10:13
 */
@Slf4j
@SpringBootApplication
public class ChatClientApplication extends Application implements CommandLineRunner, ApplicationContextAware {

    private ChannelHolder channelHolder;

    public static ConfigurableApplicationContext CONTEXT;

    private static final String SUFFIX = "$_$";

    @FXML
    private TextArea output;

    @FXML
    private TextField input;

    public static void main(String[] args) {
        SpringApplication.run(ChatClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        int port = 9101;
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            ByteBuf delimiter = Unpooled.copiedBuffer("$_$".getBytes());
                            pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192,delimiter));
                            pipeline.addLast("decoder", new StringDecoder());
                            pipeline.addLast("encoder", new StringEncoder());
                            pipeline.addLast("handler", ChatClientApplication.CONTEXT.getBean(ChatRoomClientHandler.class));
                        }
                    });
            this.channelHolder = new ChannelHolder();
            Channel channel = bootstrap.connect("127.0.0.1", port).sync().channel();
            channelHolder.setChannel(channel);
            launch(args);
            log.info(String.format("Chat room client starts successfully,server port : %d", port));
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Thread thread = new Thread(() -> {
                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    ChatClientApplication.CONTEXT.getBean(ChatClientApplication.class).getChannelHolder().getChannel().close();
                    ChatClientApplication.CONTEXT.close();
                });
                thread.setContextClassLoader(getClass().getClassLoader());
                thread.start();
            }
        });
        ClassPathResource layout = new ClassPathResource("layout.fxml");
        ClassPathResource img = new ClassPathResource("public/img/doge.jpg");
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(layout.getURL());
        fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());
        Parent root = fxmlLoader.load();
        ChatClientApplication.CONTEXT.getBean(ChatRoomClientHandler.class).setTextArea((TextArea) fxmlLoader.getNamespace().get("output"));
        primaryStage.setTitle("doge-chat");
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(root, 400, 600));
        primaryStage.getIcons().add(new Image(img.getInputStream()));
        primaryStage.show();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            CONTEXT = (ConfigurableApplicationContext) applicationContext;
        }
    }

    @FXML
    public void sendAction(ActionEvent event) throws Exception {
        ChatClientApplication.CONTEXT.getBean(ChatClientApplication.class).getChannelHolder()
                .getChannel().writeAndFlush(String.format("%s%s",input.getText(),SUFFIX));
        input.setText("");
    }

    @Data
    private class ChannelHolder{

        private Channel channel;
    }

    public ChannelHolder getChannelHolder() {
        return channelHolder;
    }

    public TextArea getOutput() {
        return output;
    }
}
