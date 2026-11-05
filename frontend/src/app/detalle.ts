import { CurrencyPipe, DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { Api, DetalleSolicitud, Estado, ETIQUETAS, mensajeDeError, Sesion, TipoPago } from './api';

const ETAPAS: { nombre: string; alcanzadaEn: Estado[] }[] = [
  { nombre: 'Recibida', alcanzadaEn: ['RECIBIDA', 'EN_REVISION', 'APROBADA', 'RECHAZADA', 'DESEMBOLSADA', 'LIQUIDADA'] },
  { nombre: 'Evaluada', alcanzadaEn: ['EN_REVISION', 'APROBADA', 'RECHAZADA', 'DESEMBOLSADA', 'LIQUIDADA'] },
  { nombre: 'Aprobada', alcanzadaEn: ['APROBADA', 'DESEMBOLSADA', 'LIQUIDADA'] },
  { nombre: 'Desembolsada', alcanzadaEn: ['DESEMBOLSADA', 'LIQUIDADA'] },
  { nombre: 'Liquidada', alcanzadaEn: ['LIQUIDADA'] },
];

const ESTADOS_CUOTA = { PENDIENTE: 'Pendiente', VENCIDA: 'Vencida', PAGADA: 'Pagada' };

@Component({
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, DecimalPipe, PercentPipe],
  template: `
    <p style="margin-bottom: 16px"><a routerLink="/creditos">← Volver a créditos</a></p>
    @if (detalle(); as d) {
      <ol class="etapas">
        @for (etapa of etapas; track etapa.nombre) {
          <li [class.hecha]="etapa.alcanzadaEn.includes(d.solicitud.estado)"
              [class.fallida]="d.solicitud.estado === 'RECHAZADA' && etapa.nombre === 'Aprobada'">
            {{ d.solicitud.estado === 'RECHAZADA' && etapa.nombre === 'Aprobada' ? 'Rechazada' : etapa.nombre }}
          </li>
        }
      </ol>

      <div class="dos-columnas">
        <aside class="columna">
          <section class="tarjeta">
            <div class="fila entre">
              <h3 style="margin: 0">Solicitud</h3>
              <span class="estado {{ d.solicitud.estado }}">{{ etiquetas[d.solicitud.estado] }}</span>
            </div>
            <p class="grande" style="margin-top: 12px">{{ d.solicitud.monto | currency }}</p>
            <dl class="datos">
              <div><dt>Plazo</dt><dd>{{ d.solicitud.plazoMeses }} meses</dd></div>
              <div><dt>Cuota</dt><dd>{{ d.solicitud.cuotaMensual === null ? '—' : (d.solicitud.cuotaMensual | currency) }}</dd></div>
              <div><dt>Tasa anual</dt><dd>{{ d.solicitud.tasaAnual === null ? '—' : (d.solicitud.tasaAnual | percent: '1.0-2') }}</dd></div>
              <div><dt>Endeudamiento</dt><dd>{{ d.solicitud.dti === null ? '—' : (d.solicitud.dti | percent: '1.0-1') }}</dd></div>
              <div><dt>Desembolso</dt><dd>{{ d.solicitud.desembolsadaEn ? (d.solicitud.desembolsadaEn | date: 'shortDate') : '—' }}</dd></div>
            </dl>
            @if (d.solicitud.motivo) {
              <p class="nota" style="margin-top: 14px">{{ d.solicitud.motivo }}</p>
            }
          </section>

          @if (puedePagar() && proximaCuota(); as cuota) {
            <section class="tarjeta">
              <h3>Pagar</h3>
              <p class="importe">Saldo pendiente</p>
              <p class="grande">{{ d.saldoPendiente | currency }}</p>
              <p class="nota" style="margin-top: 10px">
                Cuota {{ cuota.numero }} de {{ d.cuotas.length }} · vence el {{ cuota.vencimiento | date: 'shortDate' }}
                @if (cuota.recargo > 0) {
                  <br />Incluye {{ cuota.recargo | currency }} de recargo por mora.
                }
              </p>
              <div class="apilado">
                <button [disabled]="pagando()" (click)="pagar('CUOTA')">Pagar cuota · {{ cuota.total + cuota.recargo | currency }}</button>
                @if (confirmandoLiquidacion()) {
                  <p class="nota">Pagarás {{ d.importeLiquidacion | currency }} y el crédito quedará cerrado.</p>
                  <button [disabled]="pagando()" (click)="pagar('LIQUIDACION')">Confirmar liquidación</button>
                  <button class="secundario" (click)="confirmandoLiquidacion.set(false)">Cancelar</button>
                } @else {
                  <button class="secundario" (click)="confirmandoLiquidacion.set(true)">Liquidar crédito</button>
                }
              </div>
              <p class="nota" style="margin-top: 10px">Pago simulado: no se mueve dinero real.</p>
            </section>
          }

          @if (esAnalista() && d.solicitud.estado === 'EN_REVISION') {
            <form class="tarjeta" (ngSubmit)="decidir(true)">
              <h3>Decisión del analista</h3>
              <label>Motivo <input name="motivo" [(ngModel)]="motivo" required maxlength="300" /></label>
              <div class="fila">
                <button type="submit" [disabled]="!motivo">Aprobar</button>
                <button type="button" class="secundario" [disabled]="!motivo" (click)="decidir(false)">Rechazar</button>
              </div>
            </form>
          }
          @if (esAnalista() && d.solicitud.estado === 'APROBADA') {
            <div class="tarjeta">
              <button style="width: 100%" (click)="desembolsar()">Desembolsar</button>
            </div>
          }
          @if (error(); as mensaje) {
            <p class="error">{{ mensaje }}</p>
          }

          @if (d.factores.length) {
            <section class="tarjeta">
              <div class="fila entre" style="margin-bottom: 14px">
                <h3 style="margin: 0">Cómo se calculó tu score</h3>
                <strong>{{ d.solicitud.score }}</strong>
              </div>
              @for (factor of d.factores; track factor.nombre) {
                <div class="factor">
                  <div class="factor-cabecera">
                    <span>{{ factor.nombre }}</span>
                    <strong>{{ factor.puntos }} / {{ factor.maximo }}</strong>
                  </div>
                  <div class="pista"><span [style.width.%]="(factor.puntos / factor.maximo) * 100"></span></div>
                  <span class="nota">{{ factor.detalle }}</span>
                </div>
              }
            </section>
          }
        </aside>

        <div class="columna">
          @if (d.pagos.length) {
            <section class="tarjeta">
              <h3>Pagos realizados</h3>
              <div class="desplazable">
                <table>
                  <thead>
                    <tr><th>Fecha</th><th>Tipo</th><th class="num">Total</th><th class="num">Recargo</th><th class="num">Saldo</th></tr>
                  </thead>
                  <tbody>
                    @for (pago of d.pagos; track pago.id) {
                      <tr>
                        <td>{{ pago.creadoEn | date: 'short' }}</td>
                        <td>{{ pago.tipo === 'CUOTA' ? 'Cuota' : 'Liquidación' }}</td>
                        <td class="num">{{ pago.total | number: '1.2-2' }}</td>
                        <td class="num">{{ pago.recargo | number: '1.2-2' }}</td>
                        <td class="num">{{ pago.saldoRestante | number: '1.2-2' }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            </section>
          }

          @if (d.cuotas.length) {
            <section class="tarjeta">
              <h3>Plan de pagos</h3>
              <div class="desplazable">
                <table>
                  <thead>
                    <tr><th class="num">#</th><th>Vencimiento</th><th>Estado</th><th class="num">Capital</th><th class="num">Interés</th><th class="num">Cuota</th><th class="num">Saldo</th></tr>
                  </thead>
                  <tbody>
                    @for (cuota of d.cuotas; track cuota.numero) {
                      <tr>
                        <td class="num">{{ cuota.numero }}</td>
                        <td>{{ cuota.vencimiento ? (cuota.vencimiento | date: 'shortDate') : 'Al desembolsar' }}</td>
                        <td><span class="cuota-estado {{ cuota.estado }}">{{ estadosCuota[cuota.estado] }}</span></td>
                        <td class="num">{{ cuota.capital | number: '1.2-2' }}</td>
                        <td class="num">{{ cuota.interes | number: '1.2-2' }}</td>
                        <td class="num">{{ cuota.total | number: '1.2-2' }}</td>
                        <td class="num">{{ cuota.saldo | number: '1.2-2' }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            </section>
          }

          @if (d.auditoria.length) {
            <section class="tarjeta">
              <h3>Auditoría</h3>
              <ol class="linea">
                @for (evento of d.auditoria; track $index) {
                  <li>
                    <strong>{{ etiquetas[evento.estadoNuevo] }}</strong>
                    <small>{{ evento.detalle }}</small>
                    <small>{{ evento.actor }} · {{ evento.ocurridoEn | date: 'short' }}</small>
                  </li>
                }
              </ol>
            </section>
          }
        </div>
      </div>
    }
  `,
})
export class Detalle implements OnInit {
  private readonly api = inject(Api);
  private readonly sesion = inject(Sesion);

  readonly id = input.required<string>();

  protected readonly etapas = ETAPAS;
  protected readonly etiquetas = ETIQUETAS;
  protected readonly estadosCuota = ESTADOS_CUOTA;
  protected readonly esAnalista = computed(() => this.sesion.usuario()?.rol === 'ANALISTA');
  protected readonly detalle = signal<DetalleSolicitud | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly pagando = signal(false);
  protected readonly confirmandoLiquidacion = signal(false);
  protected motivo = '';

  protected readonly puedePagar = computed(
    () => !this.esAnalista() && this.detalle()?.solicitud.estado === 'DESEMBOLSADA',
  );

  protected readonly proximaCuota = computed(
    () => this.detalle()?.cuotas.find((cuota) => cuota.estado !== 'PAGADA') ?? null,
  );

  // Se conserva hasta que el pago se confirma, para que un reintento no cobre dos veces.
  private readonly clavesPago = new Map<TipoPago, string>();

  ngOnInit(): void {
    this.cargar();
  }

  protected decidir(aprobada: boolean): void {
    this.ejecutar(this.api.decidir(this.id(), aprobada, this.motivo));
  }

  protected desembolsar(): void {
    this.ejecutar(this.api.desembolsar(this.id()));
  }

  protected pagar(tipo: TipoPago): void {
    const clave = this.clavesPago.get(tipo) ?? crypto.randomUUID();
    this.clavesPago.set(tipo, clave);
    this.pagando.set(true);
    this.error.set(null);
    this.api.pagar(this.id(), tipo, clave).subscribe({
      next: () => {
        this.clavesPago.delete(tipo);
        this.pagando.set(false);
        this.confirmandoLiquidacion.set(false);
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error.set(mensajeDeError(fallo));
        this.pagando.set(false);
      },
    });
  }

  private ejecutar(operacion: Observable<unknown>): void {
    this.error.set(null);
    operacion.subscribe({
      next: () => this.cargar(),
      error: (fallo: HttpErrorResponse) => this.error.set(mensajeDeError(fallo)),
    });
  }

  private cargar(): void {
    this.api.detalle(this.id()).subscribe((detalle) => this.detalle.set(detalle));
  }
}
