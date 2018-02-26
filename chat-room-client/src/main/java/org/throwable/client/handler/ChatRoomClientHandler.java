package org.throwable.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;

/**
 * @author throwable
 * @version v1.0
 * @description
 * @since 2018/2/26 11:04
 */
@Component
public class ChatRoomClientHandler extends SimpleChannelInboundHandler<String> {

    private TextArea textArea;

    public void setTextArea(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String message) throws Exception {
        this.textArea.appendText(message);
        this.textArea.appendText("\n");
    }
}
