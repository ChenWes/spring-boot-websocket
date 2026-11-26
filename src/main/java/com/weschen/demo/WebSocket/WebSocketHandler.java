package com.weschen.demo.WebSocket;

import com.alibaba.fastjson.JSON;
import com.weschen.demo.constant.WebSocketConstants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义Websocket助手类
 */
public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static final Logger log = LoggerFactory.getLogger(WebSocketHandler.class);

    private final AttributeKey<String> channel_key = AttributeKey.valueOf(WebSocketConstants.CHANNEL_CHANNEL_ID_KEY);
    private final AttributeKey<String> lang_key = AttributeKey.valueOf(WebSocketConstants.CHANNEL_LANG_KEY);


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) {

    }

    /**
     * 频道活跃，创建连接
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        log.debug("TCP通道活跃 remote={} nettyId={}", channel.remoteAddress(), channel.id().asShortText());
    }

    /**
     * 频道不活跃，即失去连接
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        boolean tracked = ChannelHandlerPool.channelGroup.contains(channel);
        String businessChannel = channel.attr(channel_key).get();
        String lang = channel.attr(lang_key).get();
        ChannelHandlerPool.channelGroup.remove(channel);
        if (tracked) {
            log.info("客户端下线 remote={} nettyId={} channel={} lang={}",
                    channel.remoteAddress(), channel.id().asShortText(), businessChannel, lang);
        } else {
            log.debug("连接关闭且未入池 remote={} nettyId={}",
                    channel.remoteAddress(), channel.id().asShortText());
        }
    }


    /**
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        // 首次连接是FullHttpRequest，处理参数
        if (null != msg && msg instanceof FullHttpRequest) {

            //提取http请求
            FullHttpRequest request = (FullHttpRequest) msg;
            String uri = request.uri();

            // QueryStringDecoder 按 URL 规则解析，参数没有 = 或值里再出现 = 都不会切错
            QueryStringDecoder decoder = new QueryStringDecoder(uri);
            Map<String, String> paramMap = getUrlParams(decoder);
            log.debug("握手参数 remote={} nettyId={} params={}",
                    ctx.channel().remoteAddress(), ctx.channel().id().asShortText(), JSON.toJSONString(paramMap));

            //存储频道上线
            online(paramMap.get(WebSocketConstants.CHANNEL_CHANNEL_ID_KEY), paramMap.get(WebSocketConstants.CHANNEL_LANG_KEY), ctx.channel());

            // 协议处理器按路径精确匹配 /ws，查询串留在 uri 上会导致握手失败
            request.setUri(decoder.path());

        } else if (msg instanceof TextWebSocketFrame) {

            // 正常的TEXT消息类型
            // 有可能是发送过来的心跳检测
            // 也有可能是正常的文本消息
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            String text = frame.text();
            log.debug("收到消息 remote={} nettyId={} connChannel={} msgLen={} body={}",
                    ctx.channel().remoteAddress(),
                    ctx.channel().id().asShortText(),
                    ctx.channel().attr(channel_key).get(),
                    text == null ? -1 : text.length(),
                    text);


            //处理转发体
            WebSocketMessageHandler.ProcessMessage(frame.text(), ctx.channel());

        }

        super.channelRead(ctx, msg);

    }


    /**
     * 同一个参数出现多次时只保留第一个值。
     */
    private static Map<String, String> getUrlParams(QueryStringDecoder decoder) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : decoder.parameters().entrySet()) {
            List<String> values = entry.getValue();
            if (values == null || values.isEmpty()) {
                continue;
            }
            map.put(entry.getKey(), values.get(0));
        }
        return map;
    }


    /**
     * 上线一个频道，即用户端开始查看一个组件
     *
     * @param channelId
     * @param lang
     * @param channel
     */
    private void online(String channelId, String lang, Channel channel) {

        // 保存channel通道的附带信息，以频道ID和语言为标识
        channel.attr(channel_key).set(channelId);
        channel.attr(lang_key).set(lang);
        ChannelHandlerPool.channelGroup.add(channel);
        log.info("客户端上线 remote={} nettyId={} channel={} lang={}",
                channel.remoteAddress(), channel.id().asShortText(), channelId, lang);
        // 没有 channel 仍入池，方便在快照里看到这条连接。null 匹配不到任何频道；空字符串只能匹配空的 channelId
        if (channelId == null || channelId.isEmpty()) {
            log.warn("连接未携带 channel 参数已入池，不会参与按频道转发 remote={} nettyId={}",
                    channel.remoteAddress(), channel.id().asShortText());
        }
    }
}
