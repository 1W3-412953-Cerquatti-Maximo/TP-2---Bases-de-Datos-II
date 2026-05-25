import { Routes } from '@angular/router';
import { Shell } from './layout/shell';
import { Dashboard } from './pages/dashboard/dashboard';
import { NewsList } from './pages/news-list/news-list';
import { NewsDetail } from './pages/news-detail/news-detail';
import { Sources } from './pages/sources/sources';
import { Reports } from './pages/reports/reports';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { Profile } from './pages/profile/profile';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // Rutas públicas, sin layout principal (sidebar/header).
  { path: 'login', component: Login, title: 'NexoVeraz · Ingresar' },
  { path: 'register', component: Register, title: 'NexoVeraz · Crear cuenta' },

  // Rutas protegidas dentro del layout principal.
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: Dashboard, title: 'NexoVeraz · Dashboard' },
      { path: 'news', component: NewsList, title: 'NexoVeraz · Noticias' },
      { path: 'news/:id', component: NewsDetail, title: 'NexoVeraz · Detalle de noticia' },
      { path: 'sources', component: Sources, title: 'NexoVeraz · Fuentes' },
      { path: 'reports', component: Reports, title: 'NexoVeraz · Reportes' },
      { path: 'profile', component: Profile, title: 'NexoVeraz · Mi perfil' },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  { path: '**', redirectTo: 'dashboard' }
];
