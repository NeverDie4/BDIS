"use client";

import type { CurrentUser, MenuItem } from "@/types/api";
import { clearStoredToken, getStoredToken, setStoredToken } from "@/lib/auth-token";
import { create } from "zustand";

type AuthState = {
  token: string | null;
  user: CurrentUser | null;
  menus: MenuItem[];
  setAuth: (token: string, user: CurrentUser) => void;
  setUser: (user: CurrentUser | null) => void;
  setMenus: (menus: MenuItem[]) => void;
  clearAuth: () => void;
  hasPermission: (permissionCode: string) => boolean;
};

export const useAuthStore = create<AuthState>((set, get) => ({
  token: getStoredToken(),
  user: null,
  menus: [],
  setAuth: (token, user) => {
    setStoredToken(token);
    set({ token, user });
  },
  setUser: (user) => set({ user }),
  setMenus: (menus) => set({ menus }),
  clearAuth: () => {
    clearStoredToken();
    set({ token: null, user: null, menus: [] });
  },
  hasPermission: (permissionCode) => {
    const user = get().user;
    if (!user) {
      return false;
    }
    return user.permissions.includes("*") || user.permissions.includes(permissionCode);
  },
}));
