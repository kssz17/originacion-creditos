import { CurrencyPipe, PercentPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { Api, mensajeDeError, Oferta } from './api';

@Component({
  imports: [FormsModule, CurrencyPipe, PercentPipe],
  template: `
    <div class="dos-columnas">
      <aside class="columna">
        <section class="tarjeta">
          <h2>Comparar créditos</h2>
          <label>
            <span class="valor">Monto <strong>{{ monto() | currency: 'EUR' : 'symbol' : '1.0-0' }}</strong></span>
            <input
              type="range"
              name="monto"
              min="1000"
              max="100000"
              step="500"
              [ngModel]="monto()"
              (ngModelChange)="monto.set($event)"
              (change)="cargar()"
            />
          </label>
          @if (ofertas().length) {
            <dl class="datos">
              <div><dt>Tasa anual</dt><dd>{{ ofertas()[0].tasaDesde | percent }} – {{ ofertas()[0].tasaHasta | percent }}</dd></div>
              <div><dt>Plazos</dt><dd>12 a 84 meses</dd></div>
            </dl>
          }
        </section>
        <div class="consejo">
          <strong>Tu tasa final depende de tu perfil</strong>
          La evaluación analiza tus ingresos, tu endeudamiento y tu antigüedad laboral. Los importes son orientativos.
        </div>
      </aside>

      <section class="tarjeta">
        <div class="fila entre" style="margin-bottom: 14px">
          <div>
            <h2>Plazos disponibles</h2>
            <p class="nota">{{ ofertas().length }} opciones para este monto</p>
          </div>
          <label style="margin: 0">
            Ordenar por
            <select name="orden" [ngModel]="orden()" (ngModelChange)="orden.set($event)">
              <option value="plazo">Plazo más corto</option>
              <option value="cuota">Cuota más baja</option>
            </select>
          </label>
        </div>

        @if (error(); as mensaje) {
          <p class="error">{{ mensaje }}</p>
        }

        @for (oferta of ordenadas(); track oferta.plazoMeses) {
          <article class="oferta">
            <div class="plazo">{{ oferta.plazoMeses }}<small>meses</small></div>
            <div class="cifra">
              <strong>{{ oferta.cuotaDesde | currency }}</strong>
              <span>Cuota desde</span>
            </div>
            <div class="cifra opcional">
              <strong>{{ oferta.cuotaHasta | currency }}</strong>
              <span>Cuota hasta</span>
            </div>
            <div class="cifra opcional">
              <strong>{{ oferta.interesDesde | currency }}</strong>
              <span>Intereses desde</span>
            </div>
            <button class="secundario" [disabled]="enviando()" (click)="solicitar(oferta)">Solicitar</button>
          </article>
        }
      </section>
    </div>
  `,
})
export class Ofertas implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);

  protected readonly monto = signal(10000);
  protected readonly orden = signal<'plazo' | 'cuota'>('plazo');
  protected readonly ofertas = signal<Oferta[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly enviando = signal(false);

  protected readonly ordenadas = computed(() =>
    [...this.ofertas()].sort((a, b) =>
      this.orden() === 'plazo' ? a.plazoMeses - b.plazoMeses : a.cuotaDesde - b.cuotaDesde,
    ),
  );

  // Una clave por combinación: un reintento repite la clave y otra oferta usa una distinta.
  private readonly claves = new Map<string, string>();

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.api.ofertas(this.monto()).subscribe((ofertas) => this.ofertas.set(ofertas));
  }

  protected solicitar(oferta: Oferta): void {
    const combinacion = `${this.monto()}-${oferta.plazoMeses}`;
    const clave = this.claves.get(combinacion) ?? crypto.randomUUID();
    this.claves.set(combinacion, clave);
    this.enviando.set(true);
    this.error.set(null);
    this.api.crear(this.monto(), oferta.plazoMeses, clave).subscribe({
      next: (solicitud) => this.router.navigate(['/creditos', solicitud.id]),
      error: (fallo: HttpErrorResponse) => {
        this.error.set(mensajeDeError(fallo));
        this.enviando.set(false);
      },
    });
  }
}
