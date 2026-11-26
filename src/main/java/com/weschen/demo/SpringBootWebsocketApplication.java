package com.weschen.demo;

import com.weschen.demo.WebSocket.ClientsCheck;
import com.weschen.demo.WebSocket.NettyServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class SpringBootWebsocketApplication {

	private static final Logger log = LoggerFactory.getLogger(SpringBootWebsocketApplication.class);

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

			log.error("Netty WebSocket 启动失败 port=12345", e);

		}
	}

}
