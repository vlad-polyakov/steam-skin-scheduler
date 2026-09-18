package com.steam.skin.scheduler.getupdates.connector.impl;

import com.steam.skin.scheduler.getupdates.connector.logger.ChannelExceptionLogger;
import com.steam.skin.scheduler.getupdates.connector.SteamClientConnector;
import com.steam.skin.scheduler.getupdates.connector.SteamClientHandler;
import com.steam.skin.scheduler.getupdates.entity.websocket.packet.SteamPacket;
import com.steam.skin.scheduler.getupdates.packet.SteamPacketProcessor;
import com.steam.skin.scheduler.getupdates.packet.SteamWebSocketPacketDecoder;
import com.steam.skin.scheduler.getupdates.service.SteamAvailableServersService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class WebSocketSteamClientConnector implements SteamClientConnector {

    private final SteamAvailableServersService availableServersService;
    private final ApplicationEventPublisher eventPublisher;

    @Getter
    private Channel channel;

    @Getter
    private EventLoopGroup eventLoopGroup;

    @Override
    public void connect() throws Exception {
        if (channel != null && channel.isActive()) {
            return;
        }
        URI uri = new URI(returnUrl());
        this.eventLoopGroup = new NioEventLoopGroup();
        SslContext sslContext = SslContextBuilder
                .forClient().sslProvider(SslProvider.JDK).protocols("TLSv1.3", "TLSv1.2").trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpHeaders headers = new DefaultHttpHeaders();
        try {
            WebSocketClientHandshaker webSocketClientHandshaker = WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, true, headers);
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<>() {

                @Override
                protected void initChannel(Channel ch) {

                    ChannelPipeline channelPipeline = ch.pipeline();

                    SslHandler sslHandler = sslContext.newHandler(ch.alloc(), uri.getHost(), 443);
                    channelPipeline.addLast("ssl", sslHandler);

                    channelPipeline.addLast(new LoggingHandler(LogLevel.INFO));

                    channelPipeline.addLast(new HttpClientCodec());
                    channelPipeline.addLast(new HttpObjectAggregator(65536)); // Увеличили буфер с 8192 до 65536 для стабильности

                    channelPipeline.addLast(new WebSocketClientProtocolHandler(webSocketClientHandshaker));

                    channelPipeline.addLast("steamPacketDecoder", new SteamWebSocketPacketDecoder());

                    channelPipeline.addLast(new SteamClientHandler(eventPublisher));
                    channelPipeline.addLast(new ChannelExceptionLogger());
                }
            });
            channel = bootstrap.connect(uri.getHost(), 443).sync().channel();
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void disconnect() {
        if (channel != null) {
            channel.close();
            channel = null;
        }

        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
            eventLoopGroup = null;
        }
    }

    @Override
    public void sendPacket(SteamPacket steamPacket) {
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("Steam connection is closed");
        }
        channel.writeAndFlush(
                SteamPacketProcessor.toWebSocketFrame(steamPacket)
        );
    }

    private String returnUrl() {
        List<String> serverList = availableServersService.getWebSocketServers();
        int size = serverList.size();
        return serverList.get(ThreadLocalRandom.current().nextInt(size));
    }

}
