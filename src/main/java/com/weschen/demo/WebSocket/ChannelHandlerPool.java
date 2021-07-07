package com.weschen.demo.WebSocket;

import io.netty.channel.Channel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
}
