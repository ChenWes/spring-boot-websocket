package com.weschen.demo.WebSocket;

import io.netty.channel.Channel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChannelHandlerPool {

    public ChannelHandlerPool() {
    }

    public static Set<Channel> channelGroup = Collections.synchronizedSet(new HashSet<>());
}
