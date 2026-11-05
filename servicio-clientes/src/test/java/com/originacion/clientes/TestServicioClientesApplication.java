package com.originacion.clientes;

import org.springframework.boot.SpringApplication;

public class TestServicioClientesApplication {

	public static void main(String[] args) {
		SpringApplication.from(ServicioClientesApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
