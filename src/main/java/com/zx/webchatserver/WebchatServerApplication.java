package com.zx.webchatserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
public class WebchatServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebchatServerApplication.class, args);
		System.out.println("=========================================================== 启动成功 ===========================================================");
	}

}
