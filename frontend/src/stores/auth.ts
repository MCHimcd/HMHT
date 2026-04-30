import { defineStore } from "pinia";

import * as authApi from "../api/auth";
import { ApiError, setAuthToken, setUnauthorizedHandler } from "../api/client";
import type { Profile } from "../api/types";

const tokenKey = "hmht.token";
const expiresKey = "hmht.tokenExpiresAt";
const profileKey = "hmht.profile";

type AuthState = {
  token: string;
  tokenExpiresAt: number;
  profile: Profile | null;
  authReady: boolean;
};

export const useAuthStore = defineStore("auth", {
  state: (): AuthState => ({
    token: "",
    tokenExpiresAt: 0,
    profile: null,
    authReady: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token && state.tokenExpiresAt > Date.now()),
    isAdmin: (state) => state.profile?.role === "admin",
    displayName: (state) => state.profile?.userName || state.profile?.phone || "未登录",
  },
  actions: {
    bindClient() {
      setAuthToken(this.token);
      setUnauthorizedHandler(() => this.clear());
    },
    loadFromStorage() {
      this.token = localStorage.getItem(tokenKey) || "";
      this.tokenExpiresAt = Number(localStorage.getItem(expiresKey) || "0");
      const cachedProfile = localStorage.getItem(profileKey);
      this.profile = cachedProfile ? (JSON.parse(cachedProfile) as Profile) : null;
      if (this.tokenExpiresAt <= Date.now()) {
        this.clear();
      }
      this.bindClient();
    },
    persist() {
      localStorage.setItem(tokenKey, this.token);
      localStorage.setItem(expiresKey, String(this.tokenExpiresAt));
      if (this.profile) {
        localStorage.setItem(profileKey, JSON.stringify(this.profile));
      } else {
        localStorage.removeItem(profileKey);
      }
      this.bindClient();
    },
    clear() {
      this.token = "";
      this.tokenExpiresAt = 0;
      this.profile = null;
      localStorage.removeItem(tokenKey);
      localStorage.removeItem(expiresKey);
      localStorage.removeItem(profileKey);
      this.bindClient();
    },
    async restore() {
      this.loadFromStorage();
      if (!this.token) {
        this.authReady = true;
        return;
      }
      try {
        this.profile = await authApi.getProfile();
        this.persist();
      } catch (error) {
        if (error instanceof ApiError && error.code !== 0) {
          this.clear();
        }
      } finally {
        this.authReady = true;
      }
    },
    async login(username: string, password: string) {
      const result = await authApi.login(username, password);
      this.token = result.token;
      this.tokenExpiresAt = Date.now() + result.expires_in * 1000;
      this.bindClient();
      this.profile = await authApi.getProfile();
      this.persist();
    },
    async refreshProfile() {
      this.profile = await authApi.getProfile();
      this.persist();
    },
    async updateProfile(payload: Pick<Profile, "userName" | "gender" | "birthDate" | "avatarSrc">) {
      this.profile = await authApi.updateProfile(payload);
      this.persist();
    },
    async logout() {
      try {
        if (this.token) {
          await authApi.logout();
        }
      } finally {
        this.clear();
      }
    },
  },
});
