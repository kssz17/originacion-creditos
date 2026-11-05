import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api, Estado, ETIQUETAS, Sesion, Solicitud } from './api';

@Component({
  imports: [RouterLink, CurrencyPipe, DatePipe],
  template: `
    <div class="titulo">
      <h1>{{ esCliente() ? 'Mis créditos' : 'Bandeja de solicitudes' }}</h1>
    </div>
    <section class="tarjeta">
      @if (!esCliente()) {
        <div class="filtros">
          <button [class.activo]="filtro() === undefined" (click)="filtrar(undefined)">Todas</button>
          @for (estado of estados; track estado) {
            <button [class.activo]="filtro() === estado" (click)="filtrar(estado)">{{ etiquetas[estado] }}</button>
          }
        </div>
      }
      <div class="desplazable">
        <table>
          <thead>
            <tr><th>Fecha</th><th class="num">Monto</th><th class="num">Plazo</th><th class="num">Cuota</th><th>Estado</th><th class="num">Score</th></tr>
          </thead>
          <tbody>
            @for (s of solicitudes(); track s.id) {
              <tr>
                <td><a [routerLink]="['/creditos', s.id]">{{ s.creadaEn | date: 'short' }}</a></td>
                <td class="num">{{ s.monto | currency }}</td>
                <td class="num">{{ s.plazoMeses }} meses</td>
                <td class="num">{{ s.cuotaMensual === null ? '—' : (s.cuotaMensual | currency) }}</td>
                <td><span class="estado {{ s.estado }}">{{ etiquetas[s.estado] }}</span></td>
                <td class="num">{{ s.score ?? '—' }}</td>
              </tr>
            } @empty {
              <tr><td colspan="6">No hay solicitudes.</td></tr>
            }
          </tbody>
        </table>
      </div>
    </section>
  `,
})
export class Creditos implements OnInit {
  private readonly api = inject(Api);
  private readonly sesion = inject(Sesion);

  protected readonly etiquetas = ETIQUETAS;
  protected readonly estados = Object.keys(ETIQUETAS) as Estado[];
  protected readonly esCliente = computed(() => this.sesion.usuario()?.rol === 'CLIENTE');
  protected readonly solicitudes = signal<Solicitud[]>([]);
  protected readonly filtro = signal<Estado | undefined>(undefined);

  ngOnInit(): void {
    this.cargar();
  }

  protected filtrar(estado: Estado | undefined): void {
    this.filtro.set(estado);
    this.cargar();
  }

  private cargar(): void {
    this.api.listar(this.filtro()).subscribe((pagina) => this.solicitudes.set(pagina.content));
  }
}
