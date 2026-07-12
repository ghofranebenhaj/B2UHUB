import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { AnalyticsService } from '../../core/services/analytics.service';
import { AnalyticsSummary } from '../../models/analytics.model';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, NgChartsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private readonly analyticsService = inject(AnalyticsService);
  private readonly cdr = inject(ChangeDetectorRef);

  summary?: AnalyticsSummary;
  loading = true;
  error = '';
  apiUrl = environment.apiUrl;

  barChartType = 'bar' as const;
  barChartData: ChartData<'bar'> = {
    labels: ['Missions ouvertes', 'Missions en cours', 'Candidatures en attente', 'Candidatures acceptées'],
    datasets: [{
      label: 'Nombre',
      data: [0, 0, 0, 0],
      backgroundColor: ['#3b82f6', '#f59e0b', '#eab308', '#22c55e']
    }]
  };

  barChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: true,
    plugins: {
      legend: { display: false },
      title: { display: true, text: 'Vue d\'ensemble B2U-HUB' }
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: { stepSize: 1, precision: 0 }
      }
    }
  };

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.analyticsService.getSummary().subscribe({
      next: (data) => {
        this.summary = data;
        this.updateChart(data);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        this.error =
          'Impossible de joindre le backend. Démarrez-le : cd backend → mvn spring-boot:run. ' +
          `(URL : ${this.apiUrl}/analytics/summary)`;
        console.error('Dashboard error', err);
        this.cdr.detectChanges();
      }
    });
  }

  private updateChart(data: AnalyticsSummary): void {
    this.barChartData = {
      labels: ['Missions ouvertes', 'Missions en cours', 'Candidatures en attente', 'Candidatures acceptées'],
      datasets: [{
        label: 'Nombre',
        data: [
          data.missionsOuvertes,
          data.missionsEnCours,
          data.candidaturesEnAttente,
          data.candidaturesAcceptees
        ],
        backgroundColor: ['#3b82f6', '#f59e0b', '#eab308', '#22c55e']
      }]
    };
  }

}
