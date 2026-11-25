package com.weschen.demo.WebSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientsCheck implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ClientsCheck.class);

    @Override
    public void run() {

        int lastSize = -1;
        String lastSnapshot = "";

        while (true) {
            try {
                int size = ChannelHandlerPool.channelGroup.size();
                String snapshot = ChannelHandlerPool.dumpPool();

                // 数量变了或成员变了才打快照
                if (size != lastSize || !snapshot.equals(lastSnapshot)) {
                    log.debug("频道池变更 quantity={} 池={}", size, snapshot);
                } else {
                    log.debug("client quantity -> {} 频道分布={}", size, ChannelHandlerPool.poolStats());
                }

                lastSize = size;
                lastSnapshot = snapshot;

                Thread.sleep(5000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;                                   // 被中断就体面退出
            } catch (Exception e) {
                log.error("ClientsCheck 执行异常", e);     // 单轮异常不再杀死监控线程
            }
        }
    }
}
