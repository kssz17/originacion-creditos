package com.originacion.creditos;

import org.springframework.boot.SpringApplication;

public class TestServicioCreditosApplication {

	public static void main(String[] args) {
		SpringApplication.from(ServicioCreditosApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
