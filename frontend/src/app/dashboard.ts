import { CurrencyPipe, DatePipe, PercentPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Analitica, Api, ETIQUETAS, Movimiento, Resumen, Sesion, Solicitud } from './api';

const LIMITE_DTI = 0.5;

@Component({
  imports: [RouterLink, CurrencyPipe, DatePipe, PercentPipe],
  template: `
    <div class="titulo">
      <h1>Hola, <span style="text-transform: capitalize">{{ usuario()?.usuario }}</span></h1>
      <p>{{ esCliente() ? 'Este es el estado de tus créditos.' : 'Resumen de la cartera de solicitudes.' }}</p>
    </div>

    @if (resumen(); as r) {
      @if (esCliente()) {
        @if (r.cuotasVencidas > 0) {
          <div class="alerta" role="status">
            <span aria-hidden="true">⚠</span>
            <span><strong>Tienes {{ r.cuotasVencidas }} {{ r.cuotasVencidas === 1 ? 'cuota vencida' : 'cuotas vencidas' }}.</strong> Págalas cuanto antes para evitar más recargos.</span>
          </div>
        }
        <div class="dos-columnas">
          <section class="tarjeta">
            <h3>Saldo pendiente</h3>
            <p class="grande">{{ r.saldoPendiente | currency }}</p>
            <dl class="datos">
              <div><dt>Próxima cuota</dt><dd>{{ r.proximaCuota === null ? '—' : (r.proximaCuota | currency) }}</dd></div>
              <div><dt>Vencimiento</dt><dd>{{ r.proximoVencimiento ? (r.proximoVencimiento | date: 'shortDate') : '—' }}</dd></div>
              <div><dt>Cuota mensual</dt><dd>{{ r.cuotaMensual | currency }}</dd></div>
              <div><dt>Liquidados</dt><dd>{{ r.porEstado.LIQUIDADA ?? 0 }}</dd></div>
            </dl>
            @if (r.ultimoDti !== null) {
              <svg class="medidor" viewBox="0 0 120 68" role="img" [attr.aria-label]="'Endeudamiento ' + (r.ultimoDti | percent: '1.0-1')">
                <path d="M10 60a50 50 0 0 1 100 0" fill="none" stroke="#2a2a2a" stroke-width="10" stroke-linecap="round" />
                <path d="M10 60a50 50 0 0 1 100 0" fill="none" [attr.stroke]="colorMedidor()" stroke-width="10" stroke-linecap="round" pathLength="100" [attr.stroke-dasharray]="progresoMedidor() + ' 100'" />
                <text x="60" y="56" text-anchor="middle" fill="#fff" font-size="16" font-weight="700">{{ r.ultimoDti | percent: '1.0-0' }}</text>
              </svg>
              <p class="medidor-texto">Endeudamiento de tu última solicitud (límite 50%)</p>
            }
          </section>

          <div class="columna">
            <a routerLink="/ofertas" class="consejo">
              <strong>¿Necesitas financiación?</strong>
              Compara plazos y cuotas antes de solicitar tu crédito.
            </a>
            <section class="tarjeta">
              <h3>Actividad reciente</h3>
              <ol class="linea">
                @for (m of movimientos(); track $index) {
                  <li>
                    <a [routerLink]="['/creditos', m.solicitudId]">{{ etiquetas[m.estado] }}</a>
                    <small>{{ m.detalle }}</small>
                    <small>{{ m.ocurridoEn | date: 'short' }}</small>
                  </li>
                } @empty {
                  <p class="nota">Todavía no tienes actividad.</p>
                }
              </ol>
            </section>
          </div>
        </div>
      } @else {
        @if (analitica(); as a) {
          <div class="indicadores">
            <div class="tarjeta indicador"><span>Por revisar</span><strong>{{ r.porEstado.EN_REVISION ?? 0 }}</strong></div>
            <div class="tarjeta indicador"><span>Tasa de aprobación</span><strong>{{ a.tasaAprobacion | percent: '1.0-1' }}</strong></div>
            <div class="tarjeta indicador"><span>Cartera activa</span><strong>{{ a.saldoActivo | currency: 'EUR' : 'symbol' : '1.0-0' }}</strong></div>
            <div class="tarjeta indicador">
              <span>Mora · {{ a.creditosEnMora }} {{ a.creditosEnMora === 1 ? 'crédito' : 'créditos' }}</span>
              <strong>{{ a.ratioMora | percent: '1.0-1' }}</strong>
            </div>
          </div>

          <div class="graficas">
            <section class="tarjeta">
              <h3>Embudo de solicitudes</h3>
              @for (etapa of embudo(); track etapa.nombre) {
                <div class="embudo-fila">
                  <span>{{ etapa.nombre }}</span>
                  <div class="pista"><span [style.width.%]="etapa.ancho"></span></div>
                  <span class="num">{{ etapa.total }} · {{ etapa.proporcion | percent }}</span>
                </div>
              }
            </section>

            <section class="tarjeta">
              <h3>Desembolsado por mes</h3>
              <div class="columnas">
                @for (mes of meses(); track mes.mes) {
                  <div class="columna-mes" tabindex="0" [attr.aria-label]="(mes.fecha | date: 'MMMM') + ': ' + (mes.monto | currency)">
                    <i [style.height.%]="mes.alto"></i>
                    <span class="tip">{{ mes.monto | currency: 'EUR' : 'symbol' : '1.0-0' }} · {{ mes.creditos }} {{ mes.creditos === 1 ? 'crédito' : 'créditos' }}</span>
                  </div>
                }
              </div>
              <div class="ejes">
                @for (mes of meses(); track mes.mes) {
                  <span>{{ mes.fecha | date: 'MMM' }}</span>
                }
              </div>
              <details>
                <summary>Ver los datos</summary>
                <table>
                  <thead><tr><th>Mes</th><th class="num">Monto</th><th class="num">Créditos</th></tr></thead>
                  <tbody>
                    @for (mes of meses(); track mes.mes) {
                      <tr><td>{{ mes.fecha | date: 'MMMM' }}</td><td class="num">{{ mes.monto | currency }}</td><td class="num">{{ mes.creditos }}</td></tr>
                    }
                  </tbody>
                </table>
              </details>
            </section>
          </div>
        }

        <section class="tarjeta">
          <h3>Solicitudes por revisar</h3>
          <div class="desplazable">
            <table>
              <thead>
                <tr><th>Fecha</th><th class="num">Monto</th><th class="num">Plazo</th><th class="num">Score</th><th class="num">Endeudamiento</th></tr>
              </thead>
              <tbody>
                @for (s of porRevisar(); track s.id) {
                  <tr>
                    <td><a [routerLink]="['/creditos', s.id]">{{ s.creadaEn | date: 'short' }}</a></td>
                    <td class="num">{{ s.monto | currency }}</td>
                    <td class="num">{{ s.plazoMeses }} meses</td>
                    <td class="num">{{ s.score ?? '—' }}</td>
                    <td class="num">{{ s.dti === null ? '—' : (s.dti | percent: '1.0-1') }}</td>
                  </tr>
                } @empty {
                  <tr><td colspan="5">No hay solicitudes pendientes de revisión.</td></tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      }
    }
  `,
})
export class Dashboard implements OnInit {
  private readonly api = inject(Api);
  private readonly sesion = inject(Sesion);

  protected readonly etiquetas = ETIQUETAS;
  protected readonly usuario = this.sesion.usuario;
  protected readonly esCliente = computed(() => this.usuario()?.rol === 'CLIENTE');
  protected readonly resumen = signal<Resumen | null>(null);
  protected readonly analitica = signal<Analitica | null>(null);
  protected readonly movimientos = signal<Movimiento[]>([]);
  protected readonly porRevisar = signal<Solicitud[]>([]);

  protected readonly progresoMedidor = computed(() =>
    Math.min(((this.resumen()?.ultimoDti ?? 0) / LIMITE_DTI) * 100, 100),
  );

  protected readonly colorMedidor = computed(() => {
    const dti = this.resumen()?.ultimoDti ?? 0;
    if (dti > LIMITE_DTI) {
      return '#ff5f57';
    }
    return dti > 0.35 ? '#ffbb00' : '#00d27a';
  });

  protected readonly embudo = computed(() => {
    const a = this.analitica();
    if (!a) {
      return [];
    }
    const base = a.recibidas || 1;
    return [
      { nombre: 'Recibidas', total: a.recibidas },
      { nombre: 'Evaluadas', total: a.evaluadas },
      { nombre: 'Aprobadas', total: a.aprobadas },
      { nombre: 'Desembolsadas', total: a.desembolsadas },
    ].map((etapa) => ({ ...etapa, proporcion: etapa.total / base, ancho: (etapa.total / base) * 100 }));
  });

  protected readonly meses = computed(() => {
    const filas = this.analitica()?.desembolsadoPorMes ?? [];
    const maximo = Math.max(...filas.map((fila) => fila.monto), 1);
    return filas.map((fila) => ({ ...fila, fecha: `${fila.mes}-01`, alto: (fila.monto / maximo) * 100 }));
  });

  ngOnInit(): void {
    this.api.resumen().subscribe((resumen) => this.resumen.set(resumen));
    if (this.esCliente()) {
      this.api.movimientos(6).subscribe((movimientos) => this.movimientos.set(movimientos));
    } else {
      this.api.analitica().subscribe((analitica) => this.analitica.set(analitica));
      this.api.listar('EN_REVISION').subscribe((pagina) => this.porRevisar.set(pagina.content));
    }
  }
}
