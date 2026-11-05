import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';

import { Api, mensajeDeError, Usuario } from './api';

@Component({
  imports: [FormsModule],
  template: `
    <div class="tarjeta estrecha">
      <h1>Originación de Créditos</h1>
      <p class="nota" style="margin: 4px 0 20px">Elige un perfil para explorar la demo.</p>

      <div class="accesos">
        @for (perfil of perfiles; track perfil.usuario) {
          <button type="button" [disabled]="enviando()" (click)="entrarComo(perfil.usuario)">
            <strong>{{ perfil.nombre }}</strong>
            <small>{{ perfil.descripcion }}</small>
          </button>
        }
      </div>

      <p class="separador">o con tu usuario</p>

      <form (ngSubmit)="entrar()">
        <label>Usuario <input name="usuario" [(ngModel)]="usuario" required autocomplete="username" /></label>
        <label>Clave <input name="clave" type="password" [(ngModel)]="clave" required autocomplete="current-password" /></label>
        @if (error(); as mensaje) {
          <p class="error">{{ mensaje }}</p>
        }
        <button type="submit" style="width: 100%" [disabled]="enviando()">Entrar</button>
      </form>
    </div>
  `,
})
export class Login {
  private readonly api = inject(Api);
  private readonly router = inject(Router);

  protected readonly perfiles = [
    { usuario: 'ana', nombre: 'Ana', descripcion: 'Cliente con pagos' },
    { usuario: 'luis', nombre: 'Luis', descripcion: 'Cliente en revisión' },
    { usuario: 'maria', nombre: 'María', descripcion: 'Analista' },
    { usuario: 'jorge', nombre: 'Jorge', descripcion: 'Analista' },
  ];

  protected usuario = '';
  protected clave = '';
  protected readonly error = signal<string | null>(null);
  protected readonly enviando = signal(false);

  protected entrar(): void {
    this.acceder(this.api.login(this.usuario, this.clave));
  }

  protected entrarComo(usuario: string): void {
    this.acceder(this.api.accesoDemo(usuario));
  }

  private acceder(operacion: Observable<Usuario>): void {
    this.enviando.set(true);
    this.error.set(null);
    operacion.subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: (fallo: HttpErrorResponse) => {
        this.error.set(mensajeDeError(fallo));
        this.enviando.set(false);
      },
    });
  }
}
