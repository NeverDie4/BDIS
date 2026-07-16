"use client";

import type { CurrentUser, MenuItem } from "@/types/api";
import type { AuthStatus } from "@/config/routes";
import { clearStoredToken, getStoredToken, setStoredAuthTokens } from "@/lib/auth-token";
import { create } from "zustand";

type AuthState = {
  status: AuthStatus;
  token: string | null;
  user: CurrentUser | null;
  menus: MenuItem[];
  beginInitialization: (token: string) => void;
  setAuth: (token: string, refreshToken: string | undefined, user: CurrentUser) => void;
  setUser: (user: CurrentUser | null) => void;
  setMenus: (menus: MenuItem[]) => void;
  clearAuth: () => void;
  hasPermission: (permissionCode: string) => boolean;
};

export const useAuthStore = create<AuthState>((set, get) => ({
  status: "unknown",
  token: null,
  user: null,
  menus: [],
  beginInitialization: (token) => set({ token, status: "unknown" }),
  setAuth: (token, refreshToken, user) => {
    setStoredAuthTokens(token, refreshToken);
    set({ token, user, status: "authenticated" });
  },
  setUser: (user) =>
    set({
      user,
      status: user ? "authenticated" : getStoredToken() ? "unknown" : "anonymous",
    }),
  setMenus: (menus) => set({ menus }),
  clearAuth: () => {
    clearStoredToken();
    set({ token: null, user: null, menus: [], status: "anonymous" });
  },
  hasPermission: (permissionCode) => {
    const user = get().user;
    if (!user) {
      return false;
    }
    return user.permissions.includes("*") || user.permissions.includes(permissionCode);
  },
}));
