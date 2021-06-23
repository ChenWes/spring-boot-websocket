package com.weschen.demo;

import com.weschen.demo.WebSocket.ClientsCheck;
import com.weschen.demo.WebSocket.NettyServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBootWebsocketApplication {


	public static void main(String[] args) {

		SpringApplication.run(SpringBootWebsocketApplication.class, args);


		new Thread(new ClientsCheck()).start(); // 客户端检查

		try {

			/**
			 * 开启一个Netty服务器
			 */
			new NettyServer(12345).start();

		} catch (Exception e) {
			System.out.println("NettyServerError:" + e.getMessage());
		}
	}

}
