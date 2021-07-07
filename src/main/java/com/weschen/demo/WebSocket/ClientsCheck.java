package com.weschen.demo.WebSocket;

public class ClientsCheck implements Runnable {
    @Override
    public void run() {

        try {

            /**
             * 设置一个死循环，即定时器
             */
            while (true) {

                //找出频道组的数量
                int size = ChannelHandlerPool.channelGroup.size();

                //打印出来频道组的数量
                System.out.println("client quantity -> " + size);


                /**
                 * 休眠五秒
                 */
                Thread.sleep(5000);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
