import { createContext, useContext, useState, ReactNode } from "react";
import { Usuario, LoginCredentials, AuthContextType } from "../types/auth";
import { authService } from "../services/authService";

const AuthContext = createContext<AuthContextType | undefined>(undefined);

/**
 * Normaliza el rol del usuario para asegurar consistencia en la aplicación.
 * @param usuario
 */
const normalizarUsuario = (usuario: Usuario): Usuario => {
    const rol = String(usuario.rol).trim().toUpperCase();

    if (rol === "ROLE_ADMIN" || rol === "ADMINISTRADOR") {
        return { ...usuario, rol: "ADMIN" as Usuario["rol"] };
    }

    if (rol === "ROLE_RECEPCIONISTA") {
        return { ...usuario, rol: "RECEPCIONISTA" as Usuario["rol"] };
    }

    if (rol === "LIMPIEZA" || rol === "ROLE_PERSONAL_LIMPIEZA") {
        return { ...usuario, rol: "PERSONAL_LIMPIEZA" as Usuario["rol"] };
    }

    return { ...usuario, rol: rol as Usuario["rol"] };
};

/**
 * Proveedor de contexto para autenticación. Maneja el estado del usuario autenticado, funciones de login y logout, y persiste la sesión en localStorage.
 * @param children
 * @constructor
 */
export function AuthProvider({ children }: { children: ReactNode }) {
    const [usuario, setUsuario] = useState<Usuario | null>(() => {
        const stored = localStorage.getItem("usuario");
        if (!stored) return null;

        try {
            return normalizarUsuario(JSON.parse(stored) as Usuario);
        } catch {
            localStorage.removeItem("usuario");
            return null;
        }
    });

    const login = async (credentials: LoginCredentials) => {
        const usuarioAutenticado = await authService.login({
            username: credentials.username.trim(),
            password: credentials.password.trim(),
        });

        const usuarioNormalizado = normalizarUsuario(usuarioAutenticado);

        setUsuario(usuarioNormalizado);
        localStorage.setItem("usuario", JSON.stringify(usuarioNormalizado));
    };

    const logout = () => {
        setUsuario(null);
        localStorage.removeItem("usuario");
    };

    return (
        <AuthContext.Provider
            value={{
                usuario,
                login,
                logout,
                isAuthenticated: !!usuario,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

/**
 * Hook personalizado para acceder al contexto de autenticación. Asegura que se utilice dentro de un AuthProvider.
 */
export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth debe usarse dentro de AuthProvider");
    }
    return context;
}
