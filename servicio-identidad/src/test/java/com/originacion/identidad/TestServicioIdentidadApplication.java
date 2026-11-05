package com.originacion.identidad;

import org.springframework.boot.SpringApplication;

public class TestServicioIdentidadApplication {

	public static void main(String[] args) {
		SpringApplication.from(ServicioIdentidadApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
