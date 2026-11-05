package com.originacion.evaluacion;

import org.springframework.boot.SpringApplication;

public class TestServicioEvaluacionApplication {

	public static void main(String[] args) {
		SpringApplication.from(ServicioEvaluacionApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
