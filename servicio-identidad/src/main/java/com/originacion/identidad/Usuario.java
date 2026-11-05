package com.originacion.identidad;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true)
	private String usuario;

	@Column(nullable = false)
	private String nombre;

	@Column(nullable = false)
	private String email;

	@Column(name = "clave_hash", nullable = false)
	private String claveHash;

	@Column(nullable = false)
	private String rol;

	@Column(name = "cliente_id")
	private UUID clienteId;

	protected Usuario() {
	}

	public Usuario(String usuario, String nombre, String email, String claveHash, String rol, UUID clienteId) {
		this.id = UUID.randomUUID();
		this.usuario = usuario;
		this.nombre = nombre;
		this.email = email;
		this.claveHash = claveHash;
		this.rol = rol;
		this.clienteId = clienteId;
	}

	public void actualizarPerfil(String nombre, String email) {
		this.nombre = nombre;
		this.email = email;
	}

	public void cambiarClave(String claveHash) {
		this.claveHash = claveHash;
	}

	public String getUsuario() {
		return usuario;
	}

	public String getNombre() {
		return nombre;
	}

	public String getEmail() {
		return email;
	}

	public String getClaveHash() {
		return claveHash;
	}

	public String getRol() {
		return rol;
	}

	public UUID getClienteId() {
		return clienteId;
	}

}
