package com.weschen.demo.WebSocket;

public class ClientsCheck implements Runnable {
    @Override
    public void run() {

        try {
            while (true) {

                int size = ChannelHandlerPool.channelGroup.size();

                System.out.println("client quantity -> " + size);

                Thread.sleep(5000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
