package com.weschen.demo.WebSocket;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.weschen.demo.constant.LangInfoType;
import com.weschen.demo.constant.WebSocketConstants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WebSocketMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(WebSocketMessageHandler.class);

    private static final String SUPPORTED_LANGS = Arrays.stream(LangInfoType.values())
            .map(type -> StringUtils.join(type.toString().split("_"), "-"))
            .collect(Collectors.joining(","));

    public static void ProcessMessage(String message, Channel sendChannel) {
        String remote = sendChannel == null ? "unknown" : String.valueOf(sendChannel.remoteAddress());
        String nettyId = sendChannel == null ? "unknown" : sendChannel.id().asShortText();
        int msgLen = message == null ? -1 : message.length();
        String logChannelId = null;
        try {

            //首次解析消息内容
            JSONObject jsonObject = JSONObject.parseObject(message);

            //判断如果没有频道ID或者没有数据，则会不处理
            if (jsonObject == null
                    || !jsonObject.containsKey(WebSocketConstants.MESSAGE_CHANNEL_ID_KEY)
                    || !jsonObject.containsKey(WebSocketConstants.MESSAGE_DATA_KEY)) {
                log.warn("消息缺少 channelId 或 data，已忽略 remote={} nettyId={} msgLen={} body={}",
                        remote, nettyId, msgLen, message);
                return;
            }

            //获取消息体中需要发送的频道ID
            String channelId = jsonObject.getString(WebSocketConstants.MESSAGE_CHANNEL_ID_KEY);
            logChannelId = channelId;

            //获取消息体中多语言设定
            String dataString = jsonObject.getString(WebSocketConstants.MESSAGE_DATA_KEY);

            //将频道列表进行过滤
            List<Channel> channelList = getChannelByChannelID(channelId);
            if (channelList.isEmpty()) {
                log.warn("频道无在线连接，消息未转发 channel={} remote={} nettyId={} msgLen={}",
                        channelId, remote, nettyId, msgLen);
                return;
            }
            log.debug("开始转发 channel={} 目标连接数={} remote={} nettyId={} msgLen={}",
                    channelId, channelList.size(), remote, nettyId, msgLen);
            //多语言过滤key
            AttributeKey<String> langKey = AttributeKey.valueOf(WebSocketConstants.CHANNEL_LANG_KEY);

            // 每个连接单独捕获。某个连接的语言数据解析失败时，只记这一条，后面的连接继续发
            channelList.forEach(channel -> {
                try {
                    String channelLangValue = channel.attr(langKey).get();

                    if (channelLangValue == null) {
                        //非多语言的数据，直接发送至对应的频道中
                        deliver(channel, channelId, dataString, "-", -1);

                    } else {


                        //循环语言对比
                        int index = 0;
                        boolean matched = false;
                        for (LangInfoType langInfoType : LangInfoType.values()) {

                            //对比语言，有下划线与中线的不同
                            String langValue = StringUtils.join(langInfoType.toString().split("_"), "-");


                            //如果语言对比成功，则开始处理消息
                            if (langValue.equals(channelLangValue)) {
                                matched = true;

                                //将多语言数据转成数组
                                JSONArray getDataList = JSONObject.parseArray(dataString);
                                //将多语言数据取出某个索引的数据，即对应相应语言的原始数据
                                String sendData = JSONObject.toJSONString(getDataList.getJSONObject(index));

                                //将取出对应语言的数据，发送至对应的频道中
                                deliver(channel, channelId, sendData, channelLangValue, index);
                            }

                            index++;
                        }

                        if (!matched) {
                            log.warn("语言未匹配，消息未下发 channel={} remote={} nettyId={} lang={} 支持={}",
                                    channelId, channel.remoteAddress(), channel.id().asShortText(), channelLangValue, SUPPORTED_LANGS);
                        }


                    }
                } catch (Exception ex) {
                    log.error("单连接转发失败 channel={} remote={} nettyId={} lang={} dataLen={} data={}",
                            channelId,
                            channel.remoteAddress(),
                            channel.id().asShortText(),
                            channel.attr(langKey).get(),
                            dataString == null ? -1 : dataString.length(),
                            dataString,
                            ex);
                }

            });

        } catch (Exception ex) {
            log.error("消息转发失败 ex={} remote={} nettyId={} channel={} msgLen={} body={}\n频道池=\n{}",
                    ex.getClass().getSimpleName(),
                    remote,
                    nettyId,
                    logChannelId,
                    msgLen,
                    message,
                    ChannelHandlerPool.dumpPool(),
                    ex);
            /**
             * 当发生错误时，暂时不会将错误弹出，
             * 如果弹出，则会将API与Netty建立的连接强制关闭
             */
        }
    }

    /**
     * 把一帧文本写入指定连接。writeAndFlush 是异步的，成功或失败在监听器里记日志。
     * dataIndex 小于 0 表示原文直发，没有按语言拆下标。
     */
    private static void deliver(Channel channel, String channelId, String payload, String lang, int dataIndex) {
        int msgLen = payload == null ? -1 : payload.length();
        String dataIndexText = dataIndex < 0 ? "-" : Integer.toString(dataIndex);
        channel.writeAndFlush(new TextWebSocketFrame(payload)).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.debug("消息已转发 channel={} remote={} nettyId={} lang={} dataIndex={} msgLen={}",
                        channelId, channel.remoteAddress(), channel.id().asShortText(), lang, dataIndexText, msgLen);
            } else {
                log.error("消息下发失败 channel={} remote={} nettyId={} lang={} dataIndex={} msgLen={}",
                        channelId, channel.remoteAddress(), channel.id().asShortText(), lang, dataIndexText, msgLen,
                        future.cause());
            }
        });
    }


    /**
     * 通过频道ID匹配频道连接
     *
     * @param channelid
     * @return
     */
    public static List<Channel> getChannelByChannelID(String channelid) {
        return ChannelHandlerPool.findByChannelId(channelid);
    }
}
