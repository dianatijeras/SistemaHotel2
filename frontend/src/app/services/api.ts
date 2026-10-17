const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080/api";

const normalizarRol = (rol?: string) => {
    if (!rol) return undefined;

    const rolNormalizado = rol.trim().toUpperCase();

    if (rolNormalizado === "ADMIN" || rolNormalizado === "ROLE_ADMIN" || rolNormalizado === "ADMINISTRADOR") {
        return "ADMIN";
    }

    if (rolNormalizado === "RECEPCIONISTA" || rolNormalizado === "ROLE_RECEPCIONISTA") {
        return "RECEPCIONISTA";
    }

    if (
        rolNormalizado === "PERSONAL_LIMPIEZA" ||
        rolNormalizado === "LIMPIEZA" ||
        rolNormalizado === "ROLE_PERSONAL_LIMPIEZA"
    ) {
        return "PERSONAL_LIMPIEZA";
    }

    // El huésped es una entidad de dominio, no un usuario con acceso. Se conserva tal cual para
    // que el contrato refleje el rol que devuelve la API, pero nunca habilita ninguna pantalla.
    if (rolNormalizado === "HUESPED") {
        return "HUESPED";
    }

    return rolNormalizado;
};

const getStoredUser = () => {
    try {
        const stored = localStorage.getItem("usuario");
        if (!stored) return undefined;
        return JSON.parse(stored) as { id?: string; username?: string; rol?: string };
    } catch {
        return undefined;
    }
};

export async function apiRequest<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const usuario = getStoredUser();
    const userRole = normalizarRol(usuario?.rol);

    const response = await fetch(`${API_URL}${endpoint}`, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(userRole ? { "X-User-Role": userRole } : {}),
            ...(usuario?.id ? { "X-User-Id": usuario.id } : {}),
            ...(usuario?.username ? { "X-Username": usuario.username } : {}),
            ...(options.headers ?? {}),
        },
    });

    if (!response.ok) {
        let message = `Error ${response.status}`;
        try {
            const body = await response.json();
            message = body.message || body.mensaje || body.error || message;
        } catch {
            const text = await response.text();
            if (text) message = text;
        }
        throw new Error(message);
    }

    if (response.status === 204) return undefined as T;
    return response.json() as Promise<T>;
}
