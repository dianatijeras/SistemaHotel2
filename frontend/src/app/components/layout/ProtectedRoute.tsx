import { Navigate } from "react-router";
import { useAuth } from "../../context/AuthContext";
import { RolUsuario } from "../../types/auth";

interface ProtectedRouteProps {
    children: React.ReactNode;
    rolesPermitidos?: RolUsuario[];
}

export function ProtectedRoute({ children, rolesPermitidos }: ProtectedRouteProps) {
    const { isAuthenticated, usuario } = useAuth();

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    if (rolesPermitidos && usuario && !rolesPermitidos.includes(usuario.rol)) {
        return (
            <div className="flex items-center justify-center h-screen bg-gray-50">
                <div className="text-center max-w-md">
                    <div className="text-6xl mb-4">🔒</div>
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">Acceso Denegado</h1>
                    <p className="text-gray-600 mb-6">
                        No tienes permisos para acceder a esta sección
                    </p>
                    <p className="text-sm text-gray-500">
                        Tu rol actual: <span className="font-medium">{usuario.rol}</span>
                    </p>
                </div>
            </div>
        );
    }

    return <>{children}</>;
}
