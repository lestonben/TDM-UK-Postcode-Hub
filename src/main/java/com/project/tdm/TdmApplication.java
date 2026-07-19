package com.project.tdm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TdmApplication {

	public static void main(String[] args) {
		SpringApplication.run(TdmApplication.class, args);
		System.out.println("URL: http://localhost:8081/");
	}

}
