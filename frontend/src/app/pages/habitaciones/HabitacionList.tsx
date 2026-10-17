import { useEffect, useRef, useState } from "react";
import { Habitacion, EstadoHabitacion } from "../../types";
import { Card } from "../../components/ui/Card";
import { Button } from "../../components/ui/Button";
import { Modal } from "../../components/ui/Modal";
import { Sparkles } from "lucide-react";
import { hotelService } from "../../services/hotelService";

/**
 * Componente para listar y gestionar habitaciones del hotel. Muestra el estado actual de cada
 * habitación y permite cambiar su estado mediante un modal.
 *
 * Incluye la confirmación de limpieza de F-08: una habitación EN_LIMPIEZA solo se libera si la
 * recepcionista lo confirma expresamente. La acción aparece únicamente en ese estado.
 * @constructor
 */
export function HabitacionList() {
    const [habitaciones, setHabitaciones] = useState<Habitacion[]>([]);
    const [selectedHabitacion, setSelectedHabitacion] = useState<Habitacion | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [mensaje, setMensaje] = useState("");
    const [enviando, setEnviando] = useState(false);
    const envioEnCurso = useRef(false);

    const cargarHabitaciones = async () => {
        try {
            setLoading(true);
            setError("");
            setHabitaciones(await hotelService.listarHabitaciones());
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error cargando habitaciones");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        cargarHabitaciones();
    }, []);

    const getEstadoColor = (estado: EstadoHabitacion) => {
        switch (estado) {
            case EstadoHabitacion.DISPONIBLE: return "bg-green-100 text-green-800 border-green-300";
            case EstadoHabitacion.OCUPADA: return "bg-red-100 text-red-800 border-red-300";
            case EstadoHabitacion.EN_LIMPIEZA: return "bg-yellow-100 text-yellow-800 border-yellow-300";
            case EstadoHabitacion.MANTENIMIENTO: return "bg-gray-100 text-gray-800 border-gray-300";
            case EstadoHabitacion.FUERA_DE_SERVICIO: return "bg-slate-100 text-slate-800 border-slate-300";
            case EstadoHabitacion.RESERVADA: return "bg-blue-100 text-blue-800 border-blue-300";
            default: return "bg-gray-100 text-gray-800 border-gray-300";
        }
    };

    /**
     * Confirma que la limpieza de la habitación terminó y la libera. Solo aplica a habitaciones
     * EN_LIMPIEZA; el backend vuelve a validar el estado y rechaza cualquier otra transición.
     */
    const handleFinalizarLimpieza = async () => {
        if (!selectedHabitacion || envioEnCurso.current) return;
        const numero = selectedHabitacion.numero;
        if (!window.confirm(`¿Confirma que la habitación ${numero} ya fue limpiada y está disponible?`)) {
            return;
        }
        envioEnCurso.current = true;
        try {
            setEnviando(true);
            setError("");
            setMensaje("");
            await hotelService.finalizarLimpieza(numero);
            setIsModalOpen(false);
            setMensaje(`Habitación ${numero} liberada: la limpieza terminó y ya está DISPONIBLE.`);
            await cargarHabitaciones();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error finalizando la limpieza");
        } finally {
            envioEnCurso.current = false;
            setEnviando(false);
        }
    };

    const handleCambiarEstado = async (nuevoEstado: EstadoHabitacion) => {
        if (!selectedHabitacion) return;
        try {
            setError("");
            await hotelService.cambiarEstadoHabitacion(selectedHabitacion.numero, nuevoEstado);
            setIsModalOpen(false);
            await cargarHabitaciones();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error cambiando estado");
        }
    };

    const estadoPorTipo = habitaciones.reduce((acc, h) => {
        acc[h.estado] = (acc[h.estado] || 0) + 1;
        return acc;
    }, {} as Record<EstadoHabitacion, number>);

    /**
     * Estados que se pueden fijar desde esta pantalla.
     * DISPONIBLE NO aparece a propósito: una habitación solo se libera confirmando que terminó
     * la limpieza, con la acción explícita de arriba. Dejarlo aquí permitiría liberarla por una
     * vía distinta a la confirmada por recepción.
     */
    const estadosPermitidos = [
        EstadoHabitacion.RESERVADA,
        EstadoHabitacion.OCUPADA,
        EstadoHabitacion.EN_LIMPIEZA,
        EstadoHabitacion.MANTENIMIENTO,
        EstadoHabitacion.FUERA_DE_SERVICIO,
    ];

    return (
        <div className="p-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-900">Habitaciones</h1>
                <p className="text-gray-500 mt-1">Estado y gestión de habitaciones</p>
            </div>
            {error && <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">{error}</div>}
            {mensaje && <div className="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg text-green-700 text-sm">{mensaje}</div>}
            {habitaciones.some((h) => h.estado === EstadoHabitacion.EN_LIMPIEZA) && <div className="mb-6 p-4 bg-yellow-50 border border-yellow-200 rounded-lg text-yellow-800 text-sm">
                Hay habitaciones esperando limpieza. Abre una de ellas y confirma que terminó para liberarla.
            </div>}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
                <Card className="border-l-4 border-green-500"><p className="text-sm text-gray-600">Disponibles</p><p className="text-3xl font-bold text-gray-900">{estadoPorTipo[EstadoHabitacion.DISPONIBLE] || 0}</p></Card>
                <Card className="border-l-4 border-red-500"><p className="text-sm text-gray-600">Ocupadas</p><p className="text-3xl font-bold text-gray-900">{estadoPorTipo[EstadoHabitacion.OCUPADA] || 0}</p></Card>
                <Card className="border-l-4 border-yellow-500"><p className="text-sm text-gray-600">En Limpieza</p><p className="text-3xl font-bold text-gray-900">{estadoPorTipo[EstadoHabitacion.EN_LIMPIEZA] || 0}</p></Card>
                <Card className="border-l-4 border-gray-500"><p className="text-sm text-gray-600">Mantenimiento</p><p className="text-3xl font-bold text-gray-900">{estadoPorTipo[EstadoHabitacion.MANTENIMIENTO] || 0}</p></Card>
            </div>

            {loading ? <p className="text-gray-500">Cargando habitaciones...</p> : (
                <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
                    {habitaciones.map((habitacion) => (
                        <div key={habitacion.id} className={`border-2 rounded-lg p-4 cursor-pointer transition-all hover:shadow-lg ${getEstadoColor(habitacion.estado)}`} onClick={() => { setSelectedHabitacion(habitacion); setIsModalOpen(true); }}>
                            <div className="text-center">
                                <div className="text-2xl font-bold mb-2">{habitacion.numero}</div>
                                <div className="text-sm font-medium mb-1">{habitacion.tipo}</div>
                                <div className="text-xs mb-2">Cap: {habitacion.capacidad} | ${habitacion.precioNoche}/noche</div>
                                <div className="text-xs font-semibold uppercase">{habitacion.estado}</div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title={`Habitación ${selectedHabitacion?.numero}`} size="sm">
                {selectedHabitacion && <div className="space-y-4">
                    <div><p className="text-sm text-gray-600">Tipo</p><p className="font-medium text-gray-900">{selectedHabitacion.tipo}</p></div>
                    <div><p className="text-sm text-gray-600">Capacidad</p><p className="font-medium text-gray-900">{selectedHabitacion.capacidad} personas</p></div>
                    <div><p className="text-sm text-gray-600">Precio por noche</p><p className="font-medium text-gray-900">${selectedHabitacion.precioNoche}</p></div>
                    <div><p className="text-sm text-gray-600 mb-2">Estado Actual</p><p className="font-semibold text-gray-900 mb-4">{selectedHabitacion.estado}</p></div>
                    {selectedHabitacion.estado === EstadoHabitacion.EN_LIMPIEZA && <div className="p-3 bg-yellow-50 border border-yellow-200 rounded-lg mb-4">
                        <p className="text-sm text-yellow-800 mb-3">Esta habitación está esperando limpieza. Solo se libera cuando la recepción confirme que terminó.</p>
                        <Button variant="success" onClick={handleFinalizarLimpieza} disabled={enviando} className="w-full">
                            <Sparkles className="w-4 h-4 mr-2" />{enviando ? "Confirmando..." : "Confirmar limpieza y liberar"}
                        </Button>
                    </div>}
                    <div><p className="text-sm font-medium text-gray-700 mb-3">Cambiar estado a:</p><div className="space-y-2">
                        {estadosPermitidos
                            .filter((e) => e !== selectedHabitacion.estado)
                            .filter((e) => !(e === EstadoHabitacion.EN_LIMPIEZA && selectedHabitacion.estado === EstadoHabitacion.OCUPADA))
                            .map((estado) => <Button key={estado} variant="secondary" onClick={() => handleCambiarEstado(estado)} disabled={enviando} className="w-full">{estado}</Button>)}
                    </div></div>
                </div>}
            </Modal>
        </div>
    );
}