package com.weschen.demo.Controller;

import com.weschen.demo.WebSocket.ChannelHandlerPool;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class IndexController {

    private static final Logger log = LoggerFactory.getLogger(IndexController.class);

    @PostMapping("/send/single")
    public String send2User(@RequestParam(value = "channelid") String channelid, @RequestParam(value = "data") String data) {
        int dataLen = data == null ? -1 : data.length();
        try {
            //将频道列表进行过滤
            List<Channel> channelList = getChannelByChannelID(channelid);

            if (channelList.isEmpty()) {
                log.warn("单播未送达，频道不在线 channel={} dataLen={}", channelid, dataLen);
                return "频道" + channelid + "不在线！";
            }

            //过滤的频道列表，循环发送消息
            channelList.forEach(channel -> writeToChannel("单播", channelid, channel, data, dataLen));
            log.info("单播已提交 channel={} 连接数={} dataLen={}", channelid, channelList.size(), dataLen);
            return "success";
        } catch (RuntimeException e) {
            log.error("单播处理失败 channel={} dataLen={}\n频道池=\n{}", channelid, dataLen, ChannelHandlerPool.dumpPool(), e);
            throw e;
        }
    }


    @PostMapping("/send/group")
    public String send2Users(@RequestParam(value = "channelid") List<String> channelIdList,
                             @RequestParam(value = "data") String data) {
        int dataLen = data == null ? -1 : data.length();
        try {
            List<String> offline = new ArrayList<>();
            int onlineChannels = 0;
            int connCount = 0;

            //一个组用户的发送消息
            for (String channelid : channelIdList) {

                //将频道列表进行过滤
                List<Channel> channelList = getChannelByChannelID(channelid);

                if (channelList.isEmpty()) {
                    offline.add(channelid);
                    continue;
                }
                onlineChannels++;
                connCount += channelList.size();
                //过滤的频道列表，循环发送消息
                channelList.forEach(channel -> writeToChannel("组播", channelid, channel, data, dataLen));

            }

            if (!offline.isEmpty()) {
                log.warn("组播部分频道不在线 offline={} dataLen={}", offline, dataLen);
            }
            log.info("组播已提交 请求频道数={} 在线频道数={} 连接数={} 离线频道数={} dataLen={}",
                    channelIdList.size(), onlineChannels, connCount, offline.size(), dataLen);
            return "success";
        } catch (RuntimeException e) {
            log.error("组播处理失败 channels={} dataLen={}\n频道池=\n{}", channelIdList, dataLen, ChannelHandlerPool.dumpPool(), e);
            throw e;
        }
    }

    private void writeToChannel(String scene, String channelid, Channel channel, String data, int dataLen) {
        channel.writeAndFlush(new TextWebSocketFrame(data)).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.debug("{}写入成功 channel={} remote={} nettyId={} dataLen={}",
                        scene, channelid, channel.remoteAddress(), channel.id().asShortText(), dataLen);
            } else {
                log.error("{}下发失败 channel={} remote={} nettyId={} dataLen={}",
                        scene, channelid, channel.remoteAddress(), channel.id().asShortText(), dataLen, future.cause());
            }
        });
    }



    public List<Channel> getChannelByChannelID(String channelid) {
        return ChannelHandlerPool.findByChannelId(channelid);
    }
}
