export type ThemePreference = 'dark' | 'light';

export interface CurrentUser {
  id: string;
  username: string;
  email: string;
  displayName: string;
  role: string;
  themePreference: ThemePreference;
}

export interface AuthResponse {
  token: string;
  user: CurrentUser;
}

export interface RegisterRequest {
  username: string;
  email: string;
  displayName: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}
