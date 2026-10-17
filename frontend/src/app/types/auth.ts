export enum RolUsuario {
    ADMIN = 'ADMIN',
    RECEPCIONISTA = 'RECEPCIONISTA',
    PERSONAL_LIMPIEZA = 'PERSONAL_LIMPIEZA',
    /**
     * Rol de dominio, NO de acceso. Un huésped se registra como persona que se aloja, pero nunca
     * inicia sesión ni accede a ninguna pantalla. Se incluye solo para que el contrato refleje
     * fielmente el que devuelve la API (GET /api/huespedes -> "rol": "HUESPED").
     */
    HUESPED = 'HUESPED'
}

export interface Usuario {
    id: string;
    username: string;
    nombre: string;
    apellido: string;
    rol: RolUsuario;
    activo: boolean;
    fechaCreacion?: string;
}

export interface LoginCredentials {
    username: string;
    password: string;
}

export interface AuthContextType {
    usuario: Usuario | null;
    login: (credentials: LoginCredentials) => Promise<void>;
    logout: () => void;
    isAuthenticated: boolean;
}

export enum TurnoRecepcionista {
    MANANA = 'MANANA',
    TARDE = 'TARDE',
    NOCHE = 'NOCHE'
}

export interface NuevoRecepcionistaRequest {
    nombre: string;
    apellido: string;
    turno: TurnoRecepcionista;
    username: string;
    password: string;
}

export interface NuevoPersonalLimpiezaRequest {
    nombre: string;
    apellido: string;
}
