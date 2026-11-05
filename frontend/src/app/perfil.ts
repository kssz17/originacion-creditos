import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Api, DatosCliente, mensajeDeError, Perfil } from './api';

@Component({
  imports: [FormsModule, CurrencyPipe],
  template: `
    <div class="titulo">
      <h1>Mi perfil</h1>
    </div>
    @if (perfil(); as p) {
      <div class="dos-columnas">
        <aside class="columna">
          <section class="tarjeta" style="text-align: center">
            <span class="icono avatar" style="width: 76px; height: 76px; font-size: 1.8rem">{{ p.usuario[0].toUpperCase() }}</span>
            <h2 style="margin-top: 12px">{{ p.nombre }}</h2>
            <p class="nota">{{ p.usuario }} · {{ p.rol === 'CLIENTE' ? 'Cliente' : 'Analista' }}</p>
          </section>
          @if (cliente(); as c) {
            <section class="tarjeta">
              <h3>Datos financieros</h3>
              <dl class="datos" style="margin: 0">
                <div><dt>Documento</dt><dd>{{ c.documento }}</dd></div>
                <div><dt>Antigüedad</dt><dd>{{ c.antiguedadLaboralMeses }} meses</dd></div>
                <div><dt>Ingresos</dt><dd>{{ c.ingresosMensuales | currency }}</dd></div>
                <div><dt>Deuda mensual</dt><dd>{{ c.deudaMensual | currency }}</dd></div>
              </dl>
            </section>
          }
        </aside>

        <div class="columna">
          <form class="tarjeta" (ngSubmit)="guardar()">
            <h3>Datos personales</h3>
            <label>Nombre <input name="nombre" [(ngModel)]="nombre" required maxlength="120" autocomplete="name" /></label>
            <label>Correo <input name="email" type="email" [(ngModel)]="email" required maxlength="120" autocomplete="email" /></label>
            @if (avisoPerfil(); as aviso) {
              <p [class]="aviso.ok ? 'exito' : 'error'">{{ aviso.texto }}</p>
            }
            <button type="submit">Guardar cambios</button>
          </form>

          <form id="seguridad" class="tarjeta" (ngSubmit)="cambiarClave()">
            <h3>Cambiar clave</h3>
            <label>Clave actual <input name="actual" type="password" [(ngModel)]="claveActual" required autocomplete="current-password" /></label>
            <label>Clave nueva <input name="nueva" type="password" [(ngModel)]="claveNueva" required minlength="8" maxlength="72" autocomplete="new-password" /></label>
            @if (avisoClave(); as aviso) {
              <p [class]="aviso.ok ? 'exito' : 'error'">{{ aviso.texto }}</p>
            }
            <button type="submit" [disabled]="claveNueva.length < 8">Actualizar clave</button>
          </form>
        </div>
      </div>
    }
  `,
})
export class PerfilUsuario implements OnInit {
  private readonly api = inject(Api);

  protected readonly perfil = signal<Perfil | null>(null);
  protected readonly cliente = signal<DatosCliente | null>(null);
  protected readonly avisoPerfil = signal<{ ok: boolean; texto: string } | null>(null);
  protected readonly avisoClave = signal<{ ok: boolean; texto: string } | null>(null);
  protected nombre = '';
  protected email = '';
  protected claveActual = '';
  protected claveNueva = '';

  ngOnInit(): void {
    this.api.perfil().subscribe((perfil) => {
      this.mostrar(perfil);
      if (perfil.clienteId) {
        this.api.cliente(perfil.clienteId).subscribe((cliente) => this.cliente.set(cliente));
      }
    });
  }

  protected guardar(): void {
    this.api.actualizarPerfil(this.nombre, this.email).subscribe({
      next: (perfil) => {
        this.mostrar(perfil);
        this.avisoPerfil.set({ ok: true, texto: 'Perfil actualizado.' });
      },
      error: (fallo: HttpErrorResponse) =>
        this.avisoPerfil.set({ ok: false, texto: mensajeDeError(fallo) }),
    });
  }

  protected cambiarClave(): void {
    this.api.cambiarClave(this.claveActual, this.claveNueva).subscribe({
      next: () => {
        this.claveActual = '';
        this.claveNueva = '';
        this.avisoClave.set({ ok: true, texto: 'Clave actualizada.' });
      },
      error: (fallo: HttpErrorResponse) =>
        this.avisoClave.set({ ok: false, texto: mensajeDeError(fallo) }),
    });
  }

  private mostrar(perfil: Perfil): void {
    this.perfil.set(perfil);
    this.nombre = perfil.nombre;
    this.email = perfil.email;
  }
}
