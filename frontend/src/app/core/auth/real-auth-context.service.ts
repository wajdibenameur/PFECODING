import { Injectable, computed, signal } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { AuthContextPort, AuthUser } from '../../core/auth/auth-context.port';

@Injectable()
export class RealAuthContextService implements AuthContextPort {
  private readonly _isAuthenticated$ = new BehaviorSubject<boolean>(false);
  private readonly _user$ = new BehaviorSubject<AuthUser | null>(null);
  private readonly accessToken = signal<string | null>(null);
  private readonly refreshToken = signal<string | null>(null);

  readonly isAuthenticated$ = this._isAuthenticated$.asObservable();
  readonly user$ = this._user$.asObservable();

  constructor() {
    // Load from localStorage on init
    const storedAccessToken = localStorage.getItem('accessToken');
    const storedRefreshToken = localStorage.getItem('refreshToken');

    if (storedAccessToken) {
      this.setTokens(storedAccessToken, storedRefreshToken);
    }
  }

  getAccessToken(): string | null {
    return this.accessToken();
  }

  setTokens(accessToken: string, refreshToken: string | null): void {
    this.accessToken.set(accessToken);
    this.refreshToken.set(refreshToken);

    localStorage.setItem('accessToken', accessToken);
    if (refreshToken) {
      localStorage.setItem('refreshToken', refreshToken);
    }

    // Decode user from token
    const user = this.decodeUserFromToken(accessToken);
    this._user$.next(user);
    this._isAuthenticated$.next(true);
  }

  logout(): void {
    this.accessToken.set(null);
    this.refreshToken.set(null);
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    this._user$.next(null);
    this._isAuthenticated$.next(false);
  }

  getRoles(): string[] {
    const user = this._user$.value;
    return user?.roles || [];
  }

  hasRole(role: string): boolean {
    return this.getRoles().includes(role);
  }

  hasPermission(permission: string): boolean {
    // Map roles to permissions based on backend logic
    const roles = this.getRoles();
    if (roles.includes('superadmin')) {
      return true; // Superadmin has all permissions
    }
    if (roles.includes('admin')) {
      return this.isAdminPermission(permission);
    }
    if (roles.includes('support')) {
      return this.isSupportPermission(permission);
    }
    if (roles.includes('viewer')) {
      return this.isViewerPermission(permission);
    }
    return false;
  }

  private decodeUserFromToken(token: string): AuthUser | null {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const roles = payload.realm_access?.roles || [];
      return {
        id: payload.sub,
        username: payload.preferred_username || payload.sub,
        roles
      };
    } catch {
      return null;
    }
  }

  private isAdminPermission(permission: string): boolean {
    const adminPermissions = [
      'VIEW_DASHBOARD', 'VIEW_METRICS', 'VIEW_ALERTS', 'VIEW_LOGS', 'EXPORT_DASHBOARD', 'REFRESH_DASHBOARD',
      'VIEW_HOSTS', 'MANAGE_HOSTS', 'EDIT_HOST', 'DELETE_HOST',
      'VIEW_TICKETS', 'VIEW_ALL_TICKETS', 'CREATE_TICKET', 'EDIT_TICKET', 'DELETE_TICKET', 'ASSIGN_TICKET', 'VALIDATE_TICKET',
      'ADD_COMMENT', 'EDIT_COMMENT', 'DELETE_COMMENT',
      'VIEW_USERS', 'EDIT_USER', 'DELETE_USER', 'ACTIVATE_USER', 'DEACTIVATE_USER', 'MANAGE_USERS',
      'VIEW_ROLES', 'MANAGE_ROLES', 'ASSIGN_ROLE_TO_USER', 'REMOVE_ROLE_FROM_USER'
    ];
    return adminPermissions.includes(permission);
  }

  private isSupportPermission(permission: string): boolean {
    const supportPermissions = [
      'VIEW_DASHBOARD', 'VIEW_METRICS', 'VIEW_ALERTS', 'VIEW_LOGS', 'REFRESH_DASHBOARD',
      'VIEW_HOSTS',
      'VIEW_TICKETS', 'VIEW_ASSIGNED_TICKETS', 'CREATE_TICKET', 'EDIT_TICKET', 'ASSIGN_TICKET',
      'ADD_COMMENT', 'EDIT_COMMENT'
    ];
    return supportPermissions.includes(permission);
  }

  private isViewerPermission(permission: string): boolean {
    const viewerPermissions = [
      'VIEW_DASHBOARD', 'VIEW_METRICS', 'VIEW_ALERTS', 'VIEW_LOGS',
      'VIEW_HOSTS',
      'VIEW_TICKETS', 'VIEW_ASSIGNED_TICKETS'
    ];
    return viewerPermissions.includes(permission);
  }
}