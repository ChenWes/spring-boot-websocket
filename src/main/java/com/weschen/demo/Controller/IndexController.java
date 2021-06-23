package com.weschen.demo.Controller;

import com.weschen.demo.WebSocket.ChannelHandlerPool;
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
    public String send2User(@RequestParam(value = "uid") String uid, @RequestParam(value = "data") String data) {
        List<Channel> channelList = getChannelByName(uid);
        if (channelList.size() <= 0) {
            return "用户" + uid + "不在线！";
        }
        channelList.forEach(channel -> channel.writeAndFlush(new TextWebSocketFrame(data)));
        return "success";
    }


    @PostMapping("/send/group")
    public String send2Users(@RequestParam(value = "uid") List<String> userIdList,
                             @RequestParam(value = "data") String data) {
        userIdList.forEach(id -> {
            List<Channel> channelList = getChannelByName(id);
            if (channelList.size() <= 0) {
                return;
            }
            channelList.forEach(channel -> channel.writeAndFlush(new TextWebSocketFrame(data)));
        });
        return "success";
    }



    public List<Channel> getChannelByName(String name) {
        AttributeKey<String> key = AttributeKey.valueOf("user");

        return ChannelHandlerPool.channelGroup.stream().filter(channel -> channel.attr(key).get().equals(name))
                .collect(Collectors.toList());
    }
}
