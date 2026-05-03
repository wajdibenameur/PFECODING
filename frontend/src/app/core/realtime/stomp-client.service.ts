import { Inject, Injectable } from '@angular/core';
import { Client, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter, switchMap } from 'rxjs/operators';
import { APP_CONFIG, AppConfig } from '../config/app-config.token';
import { AUTH_CONTEXT, AuthContextPort } from '../auth/auth-context.port';
import { RealtimeConnectionStore } from './realtime-connection.store';

@Injectable({ providedIn: 'root' })
export class StompClientService {
  private client: Client | null = null;
  private readonly connected$ = new BehaviorSubject<boolean>(false);

  constructor(
    @Inject(APP_CONFIG) private readonly config: AppConfig,
    @Inject(AUTH_CONTEXT) private readonly authContext: AuthContextPort,
    private readonly connectionStore: RealtimeConnectionStore
  ) {}

  subscribe<T>(topic: string): Observable<T> {
    this.ensureConnected();

    return this.connected$.pipe(
      filter((connected) => connected),
      switchMap(
        () =>
          new Observable<T>((observer) => {
            if (!this.client) {
              observer.error(new Error('STOMP client is not connected'));
              return;
            }

            const subscription: StompSubscription = this.client.subscribe(topic, (message) => {
              try {
                observer.next(JSON.parse(message.body) as T);
              } catch (error) {
                observer.error(error);
              }
            });

            return () => subscription.unsubscribe();
          })
      )
    );
  }

  private ensureConnected(): void {
    if (this.client) {
      return;
    }

    const wsUrl = `${this.config.monitoringApiUrl}/ws`;
    const token = this.authContext.getAccessToken();
    if (!token) {
      this.connectionStore.setError('Authentication token is required for realtime connection');
      return;
    }

    this.connectionStore.setConnecting();

    const client = new Client({
      webSocketFactory: () => new SockJS(`${wsUrl}?access_token=${encodeURIComponent(token)}`),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 5000
    });

    client.onConnect = () => {
      this.connectionStore.setConnected();
      this.connected$.next(true);
    };

    client.onStompError = (frame) => {
      const message = frame.headers['message'] ?? 'STOMP error';
      this.connectionStore.setError(message);
      this.connected$.next(false);
    };

    client.onWebSocketError = () => {
      this.connectionStore.setError('WebSocket transport error');
      this.connected$.next(false);
    };

    client.onWebSocketClose = () => {
      this.connectionStore.setDisconnected();
      this.connected$.next(false);
    };

    client.activate();
    this.client = client;
  }
}
