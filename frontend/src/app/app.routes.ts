import { inject } from '@angular/core';
import { CanActivateFn, Router, Routes } from '@angular/router';

import { Sesion } from './api';
import { Creditos } from './creditos';
import { Dashboard } from './dashboard';
import { Detalle } from './detalle';
import { Login } from './login';
import { Movimientos } from './movimientos';
import { Ofertas } from './ofertas';
import { PerfilUsuario } from './perfil';

const autenticado: CanActivateFn = () =>
  inject(Sesion).usuario() ? true : inject(Router).parseUrl('/login');

const soloCliente: CanActivateFn = () =>
  inject(Sesion).usuario()?.rol === 'CLIENTE' ? true : inject(Router).parseUrl('/');

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: '', component: Dashboard, canActivate: [autenticado] },
  { path: 'ofertas', component: Ofertas, canActivate: [autenticado, soloCliente] },
  { path: 'creditos', component: Creditos, canActivate: [autenticado] },
  { path: 'creditos/:id', component: Detalle, canActivate: [autenticado] },
  { path: 'movimientos', component: Movimientos, canActivate: [autenticado] },
  { path: 'perfil', component: PerfilUsuario, canActivate: [autenticado] },
  { path: '**', redirectTo: '' },
];
