import { Link } from "react-router";
import { AlertCircle } from "lucide-react";
import { Button } from "../components/ui/Button";

/**
 * Componente para mostrar una página de error 404 cuando la ruta no coincide con ninguna definida.
 * @constructor
 */
export function NotFound() {
    return (
        <div className="flex items-center justify-center h-screen bg-gray-50">
            <div className="text-center">
                <AlertCircle className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                <h1 className="text-4xl font-bold text-gray-900 mb-2">404</h1>
                <p className="text-gray-600 mb-6">Página no encontrada</p>
                <Link to="/">
                    <Button variant="primary">Volver al Dashboard</Button>
                </Link>
            </div>
        </div>
    );
}