package com.weschen.demo.WebSocket;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.weschen.demo.constant.LangInfoType;
import com.weschen.demo.constant.WebSocketConstants;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class WebSocketMessageHandler {

    public static void ProcessMessage(String message) {

        try {

            //首次解析消息内容
            JSONObject jsonObject = JSONObject.parseObject(message);

            //判断如果没有频道ID或者没有数据，则会不处理
            if (!jsonObject.containsKey(WebSocketConstants.MESSAGE_CHANNEL_ID_KEY) || !jsonObject.containsKey(WebSocketConstants.MESSAGE_DATA_KEY)) {
                return;
            }

            //获取消息体中需要发送的频道ID
            String channelId = jsonObject.getString(WebSocketConstants.MESSAGE_CHANNEL_ID_KEY);
            //获取消息体中多语言设定
            String dataString = jsonObject.getString(WebSocketConstants.MESSAGE_DATA_KEY);

            //将频道列表进行过滤
            List<Channel> channelList = getChannelByChannelID(channelId);

            //多语言过滤key
            AttributeKey<String> langKey = AttributeKey.valueOf(WebSocketConstants.CHANNEL_LANG_KEY);

            //过滤的频道列表，循环发送消息
            channelList.forEach(channel -> {
                String channelLangValue = channel.attr(langKey).get();

                //循环语言对比
                int index = 0;
                for (LangInfoType langInfoType : LangInfoType.values()) {

                    //对比语言，有下划线与中线的不同
                    String langValue = StringUtils.join(langInfoType.toString().split("_"), "-");


                    //如果语言对比成功，则开始处理消息
                    if (langValue.equals(channelLangValue)) {

                        //将多语言数据转成数组
                        JSONArray getDataList = JSONObject.parseArray(dataString);
                        //将多语言数据取出某个索引的数据，即对应相应语言的原始数据
                        String sendData = JSONObject.toJSONString(getDataList.getJSONObject(index));

                        //将取出对应语言的数据，发送至对应的频道中
                        channel.writeAndFlush(new TextWebSocketFrame(sendData));
                    }

                    index++;
                }

            });

        } catch (Exception ex) {
            /**
             * 当发生错误时，暂时不会将错误弹出，
             * 如果弹出，则会将API与Netty建立的连接强制关闭
             */
            return;
        }
    }


    /**
     * 通过频道ID匹配频道连接
     * @param channelid
     * @return
     */
    public static List<Channel> getChannelByChannelID(String channelid) {

        //过滤的key值
        AttributeKey<String> key = AttributeKey.valueOf(WebSocketConstants.CHANNEL_CHANNEL_ID_KEY);

        //过滤频道池
        return ChannelHandlerPool.channelGroup.stream().filter(channel -> channel.attr(key).get().equals(channelid))
                .collect(Collectors.toList());
    }
}
