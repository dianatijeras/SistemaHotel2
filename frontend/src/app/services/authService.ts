import { LoginCredentials, Usuario } from "../types/auth";
import { apiRequest } from "./api";

/**
 * Servicio de autenticación para manejar el inicio de sesión y otras operaciones relacionadas con la autenticación.
 */
export const authService = {
    login: (credentials: LoginCredentials) =>
        apiRequest<Usuario>("/auth/login", {
            method: "POST",
            body: JSON.stringify(credentials),
        }),
};