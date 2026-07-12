import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationMessage } from '../../models/ai.model';

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {
  private client: { deactivate: () => void } | null = null;
  private readonly demoUserId = 1;

  readonly messages$ = new BehaviorSubject<NotificationMessage[]>([]);

  connect(): void {
    if (this.client) return;

    // Connexion différée — n'empêche pas l'affichage si le backend est arrêté
    setTimeout(() => this.initStomp().catch(() => {}), 1500);
  }

  private async initStomp(): Promise<void> {
    const { Client } = await import('@stomp/stompjs');
    const SockJS = (await import('sockjs-client')).default;

    const stomp = new Client({
      webSocketFactory: () => new SockJS(environment.wsUrl) as WebSocket,
      reconnectDelay: 8000,
      onConnect: () => {
        stomp.subscribe(`/topic/notifications/${this.demoUserId}`, (msg) => {
          this.pushMessage(msg.body);
        });
        stomp.subscribe('/topic/notifications', (msg) => {
          this.pushMessage(msg.body);
        });
      }
    });

    stomp.activate();
    this.client = stomp;
  }

  private pushMessage(body: string): void {
    try {
      const data = JSON.parse(body) as NotificationMessage;
      if (data.utilisateurId && data.utilisateurId !== this.demoUserId) return;
      const current = this.messages$.value;
      this.messages$.next([data, ...current].slice(0, 10));
    } catch {
      /* ignore */
    }
  }

  ngOnDestroy(): void {
    this.client?.deactivate();
    this.client = null;
  }
}
