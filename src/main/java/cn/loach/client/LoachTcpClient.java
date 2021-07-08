package cn.loach.client;

import cn.loach.client.handler.LengthFieldFrameProtocolHandler;
import cn.loach.client.message.request.SingleChatRequestMessage;
import cn.loach.client.protocol.MessageDecoder;
import cn.loach.client.protocol.MessageEcoder;
import cn.loach.client.service.SingleMessageServiceIMpl;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Scanner;

@Slf4j
public class LoachTcpClient implements LoachTcpClientInterface{
    Scanner scanner = new Scanner(System.in);

    @Override
    public void init(String host, int port) {
        NioEventLoopGroup worker = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(worker);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
//                    ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                    ch.pipeline().addLast(new LengthFieldFrameProtocolHandler());
                    ch.pipeline().addLast(new MessageDecoder());
                    ch.pipeline().addLast(new MessageEcoder());
                    ch.pipeline().addLast("client handler", new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelActive(ChannelHandlerContext ctx) {
                            new Thread(() -> {
                                while (true) {
                                    System.out.println("请输入需要发送的消息");
                                    String next = scanner.next();

                                    SingleMessageServiceIMpl singleMessageServiceIMpl = SingleMessageServiceIMpl.getInstance();
                                    SingleChatRequestMessage sendMessage = singleMessageServiceIMpl.getSendMessageModel(next);

                                    ctx.writeAndFlush(sendMessage);
                                }
                            }, "client handler thread").start();
                        }

                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) {
                            log.info("客户端读取到的数据:{}", msg.toString());
                        }
                    });
                }
            });
            ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(host, port));
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException ex) {
            log.error("client error: {}", ex.getMessage());
            ex.printStackTrace();
        } finally {
            worker.shutdownGracefully();
        }
    }

}
