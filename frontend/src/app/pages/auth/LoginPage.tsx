import { useState, FormEvent } from "react";
import { useNavigate } from "react-router";
import { Hotel, Loader2, AlertCircle } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import { Button } from "../../components/ui/Button";

/**
 * Página de inicio de sesión para el sistema de gestión hotelera. Permite a los usuarios autenticarse utilizando sus credenciales. Muestra mensajes de error en caso de autenticación fallida y proporciona una interfaz amigable con un diseño moderno.
 * @constructor
 */
export function LoginPage() {
    const navigate = useNavigate();
    const { login } = useAuth();
    const [formData, setFormData] = useState({
        username: "",
        password: "",
    });
    const [error, setError] = useState("");
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setError("");
        setIsLoading(true);

        try {
            await login(formData);
            navigate("/");
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error al iniciar sesión");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-blue-50 to-blue-100 flex items-center justify-center p-4">
            <div className="w-full max-w-md">
                {/* Logo y Header */}
                <div className="text-center mb-8">
                    <div className="inline-flex items-center justify-center w-16 h-16 bg-blue-600 rounded-2xl mb-4">
                        <Hotel className="w-10 h-10 text-white" />
                    </div>
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">HotelManager</h1>
                    <p className="text-gray-600">Sistema de Gestión Hotelera</p>
                </div>

                {/* Card de Login */}
                <div className="bg-white rounded-2xl shadow-xl p-8">
                    <h2 className="text-2xl font-bold text-gray-900 mb-6">Iniciar Sesión</h2>

                    {/* Error Message */}
                    {error && (
                        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
                            <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                            <div>
                                <p className="text-sm font-medium text-red-800">Error de autenticación</p>
                                <p className="text-sm text-red-700 mt-1">{error}</p>
                            </div>
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-5">
                        {/* Usuario */}
                        <div>
                            <label htmlFor="login-usuario" className="block text-sm font-medium text-gray-700 mb-2">
                                Usuario
                            </label>
                            <input
                                id="login-usuario"
                                type="text"
                                value={formData.username}
                                onChange={(e) =>
                                    setFormData({ ...formData, username: e.target.value })
                                }
                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                                placeholder="Ingrese su usuario"
                                required
                                disabled={isLoading}
                            />
                        </div>

                        {/* Contraseña */}
                        <div>
                            <label htmlFor="login-contrasena" className="block text-sm font-medium text-gray-700 mb-2">
                                Contraseña
                            </label>
                            <input
                                id="login-contrasena"
                                type="password"
                                value={formData.password}
                                onChange={(e) =>
                                    setFormData({ ...formData, password: e.target.value })
                                }
                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                                placeholder="Ingrese su contraseña"
                                required
                                disabled={isLoading}
                            />
                        </div>

                        {/* Botón de envío */}
                        <Button
                            type="submit"
                            variant="primary"
                            size="lg"
                            disabled={isLoading}
                            className="w-full"
                        >
                            {isLoading ? (
                                <div className="flex items-center justify-center gap-2">
                                    <Loader2 className="w-5 h-5 animate-spin" />
                                    <span>Iniciando sesión...</span>
                                </div>
                            ) : (
                                "Iniciar Sesión"
                            )}
                        </Button>
                    </form>

                    {/* Demo Credentials */}
                    <div className="mt-6 p-4 bg-gray-50 rounded-lg">
                        <p className="text-xs font-medium text-gray-700 mb-2">
                            Credenciales de demostración:
                        </p>
                        <div className="space-y-1 text-xs text-gray-600">
                            <p>
                                <span className="font-medium">Admin:</span> admin / admin2024
                            </p>
                            <p>
                                <span className="font-medium">Recepcionista:</span> maria.l / admin123
                            </p>
                        </div>
                    </div>
                </div>

                <p className="text-center text-sm text-gray-600 mt-6">
                    © 2026 HotelManager - Sistema de Gestión
                </p>
            </div>
        </div>
    );
}