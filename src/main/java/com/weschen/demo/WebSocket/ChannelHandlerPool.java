package com.weschen.demo.WebSocket;

import com.weschen.demo.constant.WebSocketConstants;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ChannelHandlerPool {

    /**
     * 频道池
     */
    public ChannelHandlerPool() {
    }

    /**
     * 频道池
     */
    public static Set<Channel> channelGroup = Collections.synchronizedSet(new HashSet<>());

    /**
     * 池快照：每个连接的 远端地址->channelId属性。
     * null 属性的连接会导致 getChannelByChannelID 空指针（如探针/未带参数的连接），直接标出。
     * synchronizedSet 的迭代必须手动加锁，否则并发增删连接时会抛 ConcurrentModificationException。
     */
    public static String dumpPool() {
        synchronized (channelGroup) {
            AttributeKey<String> key = AttributeKey.valueOf(WebSocketConstants.CHANNEL_CHANNEL_ID_KEY);
            return channelGroup.stream()
                    .map(ch -> {
                        Object id = ch.attr(key).get();
                        return ch.remoteAddress() + "->" + (id != null ? id : "NULL_ATTR(可疑)");
                    })
                    .collect(Collectors.joining(", ", "[", "]"));
        }
    }

    /**
     * 池统计：按channelId分组的订阅数
     */
    public static String poolStats() {
        synchronized (channelGroup) {
            AttributeKey<String> key = AttributeKey.valueOf(WebSocketConstants.CHANNEL_CHANNEL_ID_KEY);
            return channelGroup.stream()
                    .collect(Collectors.groupingBy(
                            ch -> {
                                Object id = ch.attr(key).get();
                                return id != null ? id.toString() : "NULL_ATTR(可疑)";
                            },
                            TreeMap::new,
                            Collectors.counting()))
                    .entrySet().stream()
                    .map(e -> e.getKey() + "×" + e.getValue())
                    .collect(Collectors.joining(", ", "[", "]"));
        }
    }

}
