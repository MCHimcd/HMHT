import { request } from "./client";
import type { CodeResult, LoginResult, Profile } from "./types";

export function login(username: string, password: string) {
  return request<LoginResult>("/v1/auth/login", {
    method: "POST",
    body: { username, password },
  });
}

export function sendVerificationCode(phone: string) {
  return request<CodeResult>("/v1/auth/send-verification-code", {
    method: "POST",
    body: { phone },
  });
}

export function register(phone: string, verificationCode: string, password: string) {
  return request<null>("/v1/auth/register", {
    method: "POST",
    body: { phone, verificationCode, password },
  });
}

export function forgotPassword(phone: string, verificationCode: string, newPassword: string) {
  return request<null>("/v1/auth/forgot-password", {
    method: "POST",
    body: { phone, verificationCode, newPassword },
  });
}

export function getProfile() {
  return request<Profile>("/v1/auth/profile");
}

export function updateProfile(payload: Pick<Profile, "userName" | "gender" | "birthDate" | "avatarSrc">) {
  return request<Profile>("/v1/auth/profile", {
    method: "PUT",
    body: payload,
  });
}

export function changePassword(oldPassword: string, newPassword: string) {
  return request<null>("/v1/auth/password", {
    method: "PUT",
    body: { oldPassword, newPassword },
  });
}

export function logout() {
  return request<null>("/v1/auth/logout", { method: "POST" });
}
