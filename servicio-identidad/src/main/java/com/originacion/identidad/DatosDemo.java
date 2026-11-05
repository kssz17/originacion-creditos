package com.originacion.identidad;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("demo")
class DatosDemo {

	@Bean
	ApplicationRunner crearUsuariosDemo(UsuarioRepository usuarios, PasswordEncoder encoder,
			@Value("${DEMO_CLAVE}") String clave) {
		return argumentos -> {
			if (usuarios.count() > 0) {
				return;
			}
			String hash = encoder.encode(clave);
			usuarios.save(new Usuario("ana", "Ana Demo", "ana@demo.test", hash, "CLIENTE", UUID.fromString("11111111-1111-1111-1111-111111111111")));
			usuarios.save(new Usuario("luis", "Luis Demo", "luis@demo.test", hash, "CLIENTE", UUID.fromString("22222222-2222-2222-2222-222222222222")));
			usuarios.save(new Usuario("carla", "Carla Demo", "carla@demo.test", hash, "CLIENTE", UUID.fromString("33333333-3333-3333-3333-333333333333")));
			usuarios.save(new Usuario("maria", "María Demo", "maria@demo.test", hash, "ANALISTA", null));
			usuarios.save(new Usuario("jorge", "Jorge Demo", "jorge@demo.test", hash, "ANALISTA", null));
		};
	}

}
