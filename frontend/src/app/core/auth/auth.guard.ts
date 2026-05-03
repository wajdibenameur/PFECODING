import { Injectable, inject } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AUTH_CONTEXT } from '../auth/auth-context.port';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  private readonly auth = inject(AUTH_CONTEXT);
  private readonly router = inject(Router);

  canActivate(): boolean {
    if (this.auth.getAccessToken()) {
      return true;
    }
    this.router.navigate(['/login']);
    return false;
  }
}