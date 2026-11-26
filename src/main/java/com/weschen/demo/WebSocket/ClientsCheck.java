package com.weschen.demo.WebSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientsCheck implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ClientsCheck.class);

    @Override
    public void run() {
        log.info("频道池监控已启动 intervalMs=5000");

        int lastSize = -1;
        String lastSnapshot = "";

        while (true) {
            try {
                int size = ChannelHandlerPool.channelGroup.size();
                String snapshot = ChannelHandlerPool.dumpPool();

                // 数量或成员变化才打表。快照已排序，遍历顺序变化不会被当成成员变化
                if (size != lastSize || !snapshot.equals(lastSnapshot)) {
                    log.info("频道池变更 quantity={} 分布={}\n{}", size, ChannelHandlerPool.poolStats(), snapshot);
                    lastSize = size;
                    lastSnapshot = snapshot;
                }

                Thread.sleep(5000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("频道池监控已停止 reason=interrupted");
                return;
            } catch (Exception e) {
                // 本轮失败后先等待，避免异常持续时立刻重试把错误日志打满
                log.error("频道池检查失败", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.info("频道池监控已停止 reason=interrupted");
                    return;
                }
            }
        }
    }
}
