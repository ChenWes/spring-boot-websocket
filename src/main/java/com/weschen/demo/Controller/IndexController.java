package com.weschen.demo.Controller;

import com.weschen.demo.WebSocket.ChannelHandlerPool;
import com.weschen.demo.constant.WebSocketConstants;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class IndexController {

    @PostMapping("/send/single")
    public String send2User(@RequestParam(value = "channelid") String channelid, @RequestParam(value = "data") String data) {

        //将频道列表进行过滤
        List<Channel> channelList = getChannelByChannelID(channelid);

        if (channelList.size() <= 0) {
            return "频道" + channelid + "不在线！";
        }

        //过滤的频道列表，循环发送消息
        channelList.forEach(channel -> channel.writeAndFlush(new TextWebSocketFrame(data)));

        return "success";
    }


    @PostMapping("/send/group")
    public String send2Users(@RequestParam(value = "channelid") List<String> channelIdList,
                             @RequestParam(value = "data") String data) {


        //一个组用户的发送消息
        channelIdList.forEach(channelid -> {

            //将频道列表进行过滤
            List<Channel> channelList = getChannelByChannelID(channelid);

            if (channelList.size() <= 0) {
                return;
            }
            //过滤的频道列表，循环发送消息
            channelList.forEach(channel -> channel.writeAndFlush(new TextWebSocketFrame(data)));

        });

        return "success";
    }



    public List<Channel> getChannelByChannelID(String channelid) {
        //过滤的key值
        AttributeKey<String> key = AttributeKey.valueOf(WebSocketConstants.CHANNEL_CHANNEL_ID_KEY);

        //过滤频道池
        return ChannelHandlerPool.channelGroup.stream().filter(channel -> channel.attr(key).get().equals(channelid))
                .collect(Collectors.toList());
    }
}
