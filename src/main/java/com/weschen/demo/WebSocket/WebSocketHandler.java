package com.weschen.demo.WebSocket;

import com.alibaba.fastjson.JSON;
import com.weschen.demo.constant.WebSocketConstants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 自定义Websocket助手类
 */
public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {


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
        System.out.println("与客户端建立连接，通道开启！");
    }

    /**
     * 频道不活跃，即失去连接
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("与客户端断开连接，通道关闭！");

        // 添加到channelGroup 通道组
        ChannelHandlerPool.channelGroup.remove(ctx.channel());
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

            //处理参数
            ConcurrentMap<String, String> paramMap = getUrlParams(uri);
            System.out.println("接收到连接的完整的参数是：" + JSON.toJSONString(paramMap));

            //存储频道上线
            online(paramMap.get(WebSocketConstants.CHANNEL_CHANNEL_ID_KEY), paramMap.get(WebSocketConstants.CHANNEL_LANG_KEY), ctx.channel());


            // 如果url包含参数，需要处理
            if (uri.contains("?")) {
                // 将参数处理掉，将其中的连接，直接导流至根部
                String newUri = uri.substring(0, uri.indexOf("?"));
                //http请求重定向
                request.setUri(newUri);
            }

        } else if (msg instanceof TextWebSocketFrame) {

            // 正常的TEXT消息类型
            // 有可能是发送过来的心跳检测
            // 也有可能是正常的文本消息
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            System.out.println("read0: " + frame.text());


            //处理转发体
            WebSocketMessageHandler.ProcessMessage(frame.text());

        }

        super.channelRead(ctx, msg);

    }


    /**
     * 处理参数
     *
     * @param url
     * @return
     */
    private static ConcurrentMap<String, String> getUrlParams(String url) {
        ConcurrentMap<String, String> map = new ConcurrentHashMap<>();

        //将问号替换成分号
        url = url.replace("?", ";");
        if (!url.contains(";")) {
            return map;
        }


        if (url.split(";").length > 0) {

            String[] arr = url.split(";")[1].split("&");

            //循环取出内容
            for (String s : arr) {

                String key = s.split("=")[0];
                String value = s.split("=")[1];

                map.put(key, value);
            }
            return map;

        } else {

            return map;
        }
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
    }
}
