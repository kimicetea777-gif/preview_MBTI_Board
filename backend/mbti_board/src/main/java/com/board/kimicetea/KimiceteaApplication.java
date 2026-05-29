package com.board.kimicetea;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KimiceteaApplication {

	public static void main(String[] args) {
		SpringApplication.run(KimiceteaApplication.class, args);
	}

}
