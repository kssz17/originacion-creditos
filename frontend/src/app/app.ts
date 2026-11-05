import { DatePipe } from '@angular/common';
import { Component, computed, effect, ElementRef, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

import { Api, ETIQUETAS, Movimiento, Sesion } from './api';

type Desplegable = 'avisos' | 'menu' | null;

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, DatePipe],
  host: { '(document:click)': 'cerrarSiEsFuera($event)', '(document:keydown.escape)': 'abierto.set(null)' },
  template: `
    @if (sesion.usuario(); as usuario) {
      <header class="barra">
        <a routerLink="/" class="marca" aria-label="Inicio">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <rect x="3" y="12" width="4" height="9" rx="1" /><rect x="10" y="7" width="4" height="14" rx="1" /><rect x="17" y="3" width="4" height="18" rx="1" />
          </svg>
          Originación
        </a>

        <nav class="pestanas" aria-label="Principal">
          @for (enlace of enlaces(); track enlace.ruta) {
            <a [routerLink]="enlace.ruta" routerLinkActive="activa" [routerLinkActiveOptions]="{ exact: enlace.ruta === '/' }">{{ enlace.texto }}</a>
          }
        </nav>

        <div class="acciones">
          <button class="icono" aria-label="Notificaciones" [attr.aria-expanded]="abierto() === 'avisos'" (click)="alternarAvisos()">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" /><path d="M10.3 21a1.9 1.9 0 0 0 3.4 0" />
            </svg>
            @if (sinLeer() > 0) {
              <span class="globo">{{ sinLeer() }}</span>
            }
          </button>

          <a routerLink="/perfil" class="icono avatar" aria-label="Mi perfil">{{ usuario.usuario[0].toUpperCase() }}</a>

          <button class="icono" aria-label="Menú" [attr.aria-expanded]="abierto() === 'menu'" (click)="alternar('menu')">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
              <path d="M4 7h16M4 12h16M4 17h16" />
            </svg>
          </button>

          @if (abierto() === 'avisos') {
            <div class="menu ancho">
              <h3>Notificaciones</h3>
              @for (aviso of avisos(); track $index) {
                <a [routerLink]="['/creditos', aviso.solicitudId]" (click)="abierto.set(null)">
                  {{ etiquetas[aviso.estado] }}
                  <small>{{ aviso.detalle }}</small>
                  <small>{{ aviso.ocurridoEn | date: 'short' }}</small>
                </a>
              } @empty {
                <p class="vacio">No tienes notificaciones.</p>
              }
            </div>
          }

          @if (abierto() === 'menu') {
            <div class="menu">
              @for (enlace of enlaces(); track enlace.ruta) {
                <a class="solo-movil" [routerLink]="enlace.ruta" (click)="abierto.set(null)">{{ enlace.texto }}</a>
              }
              <a routerLink="/perfil" (click)="abierto.set(null)">Mi perfil</a>
              <a routerLink="/perfil" fragment="seguridad" (click)="abierto.set(null)">Cambiar clave</a>
              <button class="salir" (click)="salir()">Cerrar sesión</button>
            </div>
          }
        </div>
      </header>
    }
    <main class="contenido"><router-outlet /></main>
  `,
})
export class App {
  protected readonly sesion = inject(Sesion);
  private readonly api = inject(Api);
  private readonly elemento = inject(ElementRef<HTMLElement>);

  protected readonly etiquetas = ETIQUETAS;
  protected readonly abierto = signal<Desplegable>(null);
  protected readonly avisos = signal<Movimiento[]>([]);
  private readonly vistoEn = signal(0);

  protected readonly enlaces = computed(() => {
    const cliente = this.sesion.usuario()?.rol === 'CLIENTE';
    return [
      { ruta: '/', texto: 'Dashboard' },
      ...(cliente ? [{ ruta: '/ofertas', texto: 'Ofertas' }] : []),
      { ruta: '/creditos', texto: 'Créditos' },
      { ruta: '/movimientos', texto: 'Movimientos' },
    ];
  });

  protected readonly sinLeer = computed(
    () => this.avisos().filter((aviso) => Date.parse(aviso.ocurridoEn) > this.vistoEn()).length,
  );

  constructor() {
    effect(() => {
      const usuario = this.sesion.usuario();
      this.vistoEn.set(usuario ? this.leerVisto(usuario.usuario) : 0);
      this.avisos.set([]);
      if (usuario) {
        this.cargarAvisos();
      }
    });
    inject(Router)
      .events.pipe(filter((evento) => evento instanceof NavigationEnd))
      .subscribe(() => this.cargarAvisos());
  }

  protected alternar(desplegable: Exclude<Desplegable, null>): void {
    this.abierto.update((actual) => (actual === desplegable ? null : desplegable));
  }

  protected alternarAvisos(): void {
    this.alternar('avisos');
    const usuario = this.sesion.usuario();
    if (usuario) {
      const ahora = Date.now();
      this.guardarVisto(usuario.usuario, ahora);
      this.vistoEn.set(ahora);
    }
  }

  protected salir(): void {
    this.abierto.set(null);
    this.sesion.cerrar();
  }

  protected cerrarSiEsFuera(evento: Event): void {
    const acciones = this.elemento.nativeElement.querySelector('.acciones');
    if (!acciones?.contains(evento.target as Node)) {
      this.abierto.set(null);
    }
  }

  private cargarAvisos(): void {
    const usuario = this.sesion.usuario();
    if (!usuario) {
      return;
    }
    // Las acciones propias no son un aviso: solo importa lo que hicieron otros.
    this.api.movimientos(10).subscribe((movimientos) =>
      this.avisos.set(
        movimientos.filter((m) => !m.propio).slice(0, 6),
      ),
    );
  }

  private leerVisto(usuario: string): number {
    try {
      return Number(localStorage.getItem(`avisos-vistos-${usuario}`)) || 0;
    } catch {
      return 0;
    }
  }

  private guardarVisto(usuario: string, instante: number): void {
    try {
      localStorage.setItem(`avisos-vistos-${usuario}`, String(instante));
    } catch {
      // Sin almacenamiento, el contador solo vale durante la sesión.
    }
  }
}
