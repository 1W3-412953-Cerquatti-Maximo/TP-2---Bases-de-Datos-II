import { Routes } from '@angular/router';
import { Shell } from './layout/shell';
import { Dashboard } from './pages/dashboard/dashboard';
import { NewsList } from './pages/news-list/news-list';
import { NewsDetail } from './pages/news-detail/news-detail';
import { Sources } from './pages/sources/sources';
import { Reports } from './pages/reports/reports';

export const routes: Routes = [
  {
    path: '',
    component: Shell,
    children: [
      { path: 'dashboard', component: Dashboard, title: 'NexoVeraz · Dashboard' },
      { path: 'news', component: NewsList, title: 'NexoVeraz · Noticias' },
      { path: 'news/:id', component: NewsDetail, title: 'NexoVeraz · Detalle de noticia' },
      { path: 'sources', component: Sources, title: 'NexoVeraz · Fuentes' },
      { path: 'reports', component: Reports, title: 'NexoVeraz · Reportes' },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
