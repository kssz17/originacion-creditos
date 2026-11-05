import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, Observable, tap, throwError } from 'rxjs';

export type Estado = 'RECIBIDA' | 'EN_REVISION' | 'APROBADA' | 'RECHAZADA' | 'DESEMBOLSADA' | 'LIQUIDADA';

export type TipoPago = 'CUOTA' | 'LIQUIDACION';

export interface Usuario {
  token: string;
  usuario: string;
  rol: 'CLIENTE' | 'ANALISTA';
  clienteId: string | null;
}

export interface Solicitud {
  id: string;
  monto: number;
  plazoMeses: number;
  estado: Estado;
  score: number | null;
  dti: number | null;
  tasaAnual: number | null;
  cuotaMensual: number | null;
  motivo: string | null;
  analista: string | null;
  creadaEn: string;
  desembolsadaEn: string | null;
}

export interface Cuota {
  numero: number;
  vencimiento: string | null;
  capital: number;
  interes: number;
  total: number;
  saldo: number;
  estado: 'PENDIENTE' | 'VENCIDA' | 'PAGADA';
  recargo: number;
}

export interface Evento {
  estadoAnterior: Estado | null;
  estadoNuevo: Estado;
  actor: string;
  detalle: string;
  ocurridoEn: string;
}

export interface Pago {
  id: string;
  tipo: TipoPago;
  capital: number;
  interes: number;
  recargo: number;
  total: number;
  saldoRestante: number;
  creadoEn: string;
}

export interface Factor {
  nombre: string;
  puntos: number;
  maximo: number;
  detalle: string;
}

export interface DetalleSolicitud {
  solicitud: Solicitud;
  cuotas: Cuota[];
  auditoria: Evento[];
  pagos: Pago[];
  factores: Factor[];
  saldoPendiente: number;
  importeLiquidacion: number;
}

export interface Analitica {
  recibidas: number;
  evaluadas: number;
  aprobadas: number;
  desembolsadas: number;
  tasaAprobacion: number;
  desembolsadoPorMes: { mes: string; monto: number; creditos: number }[];
  saldoActivo: number;
  saldoEnMora: number;
  creditosEnMora: number;
  ratioMora: number;
}

export interface Oferta {
  plazoMeses: number;
  tasaDesde: number;
  tasaHasta: number;
  cuotaDesde: number;
  cuotaHasta: number;
  interesDesde: number;
}

export interface Resumen {
  porEstado: Partial<Record<Estado, number>>;
  montoDesembolsado: number;
  cuotaMensual: number;
  ultimoDti: number | null;
  saldoPendiente: number;
  cuotasVencidas: number;
  proximoVencimiento: string | null;
  proximaCuota: number | null;
}

export interface Movimiento {
  solicitudId: string;
  estado: Estado;
  actor: string | null;
  propio: boolean;
  detalle: string;
  ocurridoEn: string;
}

export interface Perfil {
  usuario: string;
  nombre: string;
  email: string;
  rol: Usuario['rol'];
  clienteId: string | null;
}

export interface DatosCliente {
  documento: string;
  ingresosMensuales: number;
  deudaMensual: number;
  antiguedadLaboralMeses: number;
}

export const ETIQUETAS: Record<Estado, string> = {
  RECIBIDA: 'Recibida',
  EN_REVISION: 'En revisión',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
  DESEMBOLSADA: 'Desembolsada',
  LIQUIDADA: 'Liquidada',
};

const CLAVE_SESION = 'sesion';

@Injectable({ providedIn: 'root' })
export class Sesion {
  private readonly router = inject(Router);
  readonly usuario = signal<Usuario | null>(this.restaurar());

  iniciar(usuario: Usuario): void {
    sessionStorage.setItem(CLAVE_SESION, JSON.stringify(usuario));
    this.usuario.set(usuario);
  }

  cerrar(): void {
    sessionStorage.removeItem(CLAVE_SESION);
    this.usuario.set(null);
    this.router.navigateByUrl('/login');
  }

  private restaurar(): Usuario | null {
    const guardada = sessionStorage.getItem(CLAVE_SESION);
    return guardada ? JSON.parse(guardada) : null;
  }
}

export const autorizacion: HttpInterceptorFn = (peticion, siguiente) => {
  const sesion = inject(Sesion);
  const token = sesion.usuario()?.token;
  const autorizada = token
    ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : peticion;
  return siguiente(autorizada).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && token) {
        sesion.cerrar();
      }
      return throwError(() => error);
    }),
  );
};

export function mensajeDeError(error: HttpErrorResponse): string {
  return error.error?.detail ?? 'No se pudo completar la operación. Inténtalo de nuevo.';
}

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);
  private readonly sesion = inject(Sesion);

  login(usuario: string, clave: string): Observable<Usuario> {
    return this.http
      .post<Usuario>('/api/auth/login', { usuario, clave })
      .pipe(tap((sesion) => this.sesion.iniciar(sesion)));
  }

  accesoDemo(usuario: string): Observable<Usuario> {
    return this.http
      .post<Usuario>('/api/auth/demo', { usuario })
      .pipe(tap((sesion) => this.sesion.iniciar(sesion)));
  }

  perfil(): Observable<Perfil> {
    return this.http.get<Perfil>('/api/auth/perfil');
  }

  actualizarPerfil(nombre: string, email: string): Observable<Perfil> {
    return this.http.put<Perfil>('/api/auth/perfil', { nombre, email });
  }

  cambiarClave(actual: string, nueva: string): Observable<void> {
    return this.http.post<void>('/api/auth/clave', { actual, nueva });
  }

  cliente(id: string): Observable<DatosCliente> {
    return this.http.get<DatosCliente>(`/api/clientes/${id}`);
  }

  resumen(): Observable<Resumen> {
    return this.http.get<Resumen>('/api/solicitudes/resumen');
  }

  analitica(): Observable<Analitica> {
    return this.http.get<Analitica>('/api/solicitudes/analitica');
  }

  ofertas(monto: number): Observable<Oferta[]> {
    return this.http.get<Oferta[]>('/api/solicitudes/ofertas', { params: { monto } });
  }

  movimientos(limite = 30): Observable<Movimiento[]> {
    return this.http.get<Movimiento[]>('/api/solicitudes/movimientos', { params: { limite } });
  }

  listar(estado?: Estado): Observable<{ content: Solicitud[] }> {
    const params: Record<string, string> = estado ? { estado } : {};
    return this.http.get<{ content: Solicitud[] }>('/api/solicitudes', { params });
  }

  crear(monto: number, plazoMeses: number, idempotencyKey: string): Observable<Solicitud> {
    return this.http.post<Solicitud>(
      '/api/solicitudes',
      { monto, plazoMeses },
      { headers: { 'Idempotency-Key': idempotencyKey } },
    );
  }

  detalle(id: string): Observable<DetalleSolicitud> {
    return this.http.get<DetalleSolicitud>(`/api/solicitudes/${id}`);
  }

  decidir(id: string, aprobada: boolean, motivo: string): Observable<Solicitud> {
    return this.http.post<Solicitud>(`/api/solicitudes/${id}/decision`, { aprobada, motivo });
  }

  pagar(id: string, tipo: TipoPago, idempotencyKey: string): Observable<Pago> {
    return this.http.post<Pago>(
      `/api/solicitudes/${id}/pagos`,
      { tipo },
      { headers: { 'Idempotency-Key': idempotencyKey } },
    );
  }

  desembolsar(id: string): Observable<Solicitud> {
    return this.http.post<Solicitud>(`/api/solicitudes/${id}/desembolso`, {});
  }
}
