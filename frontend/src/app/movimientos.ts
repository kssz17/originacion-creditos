import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api, ETIQUETAS, Movimiento } from './api';

@Component({
  imports: [RouterLink, DatePipe],
  template: `
    <div class="titulo">
      <h1>Movimientos</h1>
      <p>Cada cambio de estado queda registrado de forma permanente.</p>
    </div>
    <section class="tarjeta">
      <div class="desplazable">
        <table>
          <thead>
            <tr><th>Fecha</th><th>Estado</th><th>Detalle</th><th>Responsable</th></tr>
          </thead>
          <tbody>
            @for (m of movimientos(); track $index) {
              <tr>
                <td><a [routerLink]="['/creditos', m.solicitudId]">{{ m.ocurridoEn | date: 'short' }}</a></td>
                <td><span class="estado {{ m.estado }}">{{ etiquetas[m.estado] }}</span></td>
                <td>{{ m.detalle }}</td>
                <td>{{ m.actor ?? 'Entidad' }}</td>
              </tr>
            } @empty {
              <tr><td colspan="4">Todavía no hay movimientos.</td></tr>
            }
          </tbody>
        </table>
      </div>
    </section>
  `,
})
export class Movimientos implements OnInit {
  private readonly api = inject(Api);

  protected readonly etiquetas = ETIQUETAS;
  protected readonly movimientos = signal<Movimiento[]>([]);

  ngOnInit(): void {
    this.api.movimientos(50).subscribe((movimientos) => this.movimientos.set(movimientos));
  }
}
