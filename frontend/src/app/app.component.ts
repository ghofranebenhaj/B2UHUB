import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NotificationService } from './core/services/notification.service';
import { SessionService, UserRole } from './core/services/session.service';
import { NotificationMessage } from './models/ai.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  private readonly notificationService = inject(NotificationService);
  readonly session = inject(SessionService);
  title = 'B2U-HUB';
  notifications: NotificationMessage[] = [];

  ngOnInit(): void {
    this.notificationService.connect();
    this.notificationService.messages$.subscribe((msgs) => (this.notifications = msgs));
  }

  onRoleChange(role: UserRole): void {
    this.session.setRole(role);
  }
}
