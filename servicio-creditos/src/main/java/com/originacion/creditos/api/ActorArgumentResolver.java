package com.originacion.creditos.api;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.originacion.creditos.aplicacion.Actor;

@Configuration
class ActorArgumentResolver implements HandlerMethodArgumentResolver, WebMvcConfigurer {

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(this);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.getParameterType() == Actor.class;
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest request, WebDataBinderFactory binderFactory) {
		String usuario = request.getHeader("X-Usuario");
		String rol = request.getHeader("X-Rol");
		if (usuario == null || rol == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		String clienteId = request.getHeader("X-Cliente-Id");
		return new Actor(usuario, rol, clienteId == null ? null : UUID.fromString(clienteId));
	}

}
