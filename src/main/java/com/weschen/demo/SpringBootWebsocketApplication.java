package com.weschen.demo;

import com.weschen.demo.WebSocket.ClientsCheck;
import com.weschen.demo.WebSocket.NettyServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBootWebsocketApplication {

	public static void main(String[] args) {


		/**
		 * 运行的正常的SpringBoot服务器
		 */
		SpringApplication.run(SpringBootWebsocketApplication.class, args);




		/**
		 * 客户端检查
		 */
		new Thread(new ClientsCheck()).start();

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
