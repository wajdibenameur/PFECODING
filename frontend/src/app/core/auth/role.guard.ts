import { Injectable, inject } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { AUTH_CONTEXT } from '../auth/auth-context.port';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  private readonly auth = inject(AUTH_CONTEXT);
  private readonly router = inject(Router);

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const requiredRoles = route.data?.['roles'] as string[] || [];
    const userRoles = this.auth.getRoles();

    if (requiredRoles.some(role => userRoles.includes(role))) {
      return true;
    }

    this.router.navigate(['/dashboard']); // Redirect to safe page
    return false;
  }
}