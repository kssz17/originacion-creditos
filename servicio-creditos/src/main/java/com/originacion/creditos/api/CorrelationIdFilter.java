package com.originacion.creditos.api;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
class CorrelationIdFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String correlationId = request.getHeader("X-Correlation-Id");
		if (correlationId != null) {
			MDC.put("correlationId", correlationId);
		}
		try {
			chain.doFilter(request, response);
		}
		finally {
			MDC.remove("correlationId");
		}
	}

}
