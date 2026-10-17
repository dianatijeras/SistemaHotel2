import { useEffect, useRef, useState } from "react";
import { Search, LogIn } from "lucide-react";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Estadia, Reserva, EstadoReserva } from "../types";
import { hotelService } from "../services/hotelService";
import { formatoFechaHoraLocal } from "../utils/formatoFechaHora";

/** Estados de reserva que permiten iniciar una estadía. Coinciden con los que valida el backend. */
const HABILITADAS = [EstadoReserva.RESERVADA, EstadoReserva.CONFIRMADA];

/** Devuelve la fecha local en formato yyyy-MM-dd, igual que el que devuelve la API. */
const hoyISO = () => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};


/**
 * Componente para gestionar el proceso de check-in de huéspedes (F-06). Permite buscar reservas
 * habilitadas, verificar los datos del huésped —incluido su documento de identidad— y registrar
 * la llegada al hotel.
 * @constructor
 */
export function CheckIn() {
    const [busqueda, setBusqueda] = useState("");
    const [reservas, setReservas] = useState<Reserva[]>([]);
    const [reservaSeleccionada, setReservaSeleccionada] = useState<Reserva | null>(null);
    const [error, setError] = useState("");
    const [mensaje, setMensaje] = useState("");
    const [registrando, setRegistrando] = useState(false);
    const [cargando, setCargando] = useState(true);

    const cargar = async () => {
        try {
            setCargando(true);
            setError("");
            const todas = await hotelService.listarReservas();
            setReservas(todas.filter((r) => HABILITADAS.includes(r.estado)));
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error cargando reservas");
        } finally {
            setCargando(false);
        }
    };

    useEffect(() => { cargar(); }, []);

    const reservasFiltradas = reservas.filter((r) =>
        `${r.codigoReserva} ${r.id} ${r.huesped?.nombre ?? ""} ${r.huesped?.apellido ?? ""} ${r.huesped?.documento ?? ""} ${r.numeroHabitacion}`.toLowerCase().includes(busqueda.toLowerCase())
    );

    /**
     * Devuelve el motivo por el que una reserva NO admite check-in, o null si sí lo admite.
     * Refleja exactamente las validaciones del backend para que la recepcionista no discovers
     * el problema solo tras pulsar el botón.
     */
    const motivoBloqueo = (r: Reserva): string | null => {
        if (!HABILITADAS.includes(r.estado)) return `La reserva está en estado ${r.estado}.`;
        const estado = r.habitacion?.estado;
        if (estado === "EN_LIMPIEZA") return "La habitación está EN_LIMPIEZA: debe pasar a DISPONIBLE antes del check-in.";
        if (estado === "MANTENIMIENTO" || estado === "FUERA_DE_SERVICIO") return `La habitación está en estado ${estado}.`;
        if (estado === "OCUPADA") return "La habitación ya está OCUPADA.";
        const hoy = hoyISO();
        if (r.fechaInicio && hoy < r.fechaInicio) return `Aún no puede registrarse: la fecha de entrada es el ${r.fechaInicio}.`;
        if (r.fechaFin && hoy >= r.fechaFin) return `La estadía termina el ${r.fechaFin}: no se puede registrar la llegada ese día.`;
        if (!r.huesped?.documento) return "El huésped no tiene documento de identidad registrado.";
        return null;
    };

    /** Una reserva es habilitada si no existe ningún motivo de bloqueo. */
    const esHabilitada = (r: Reserva) => motivoBloqueo(r) === null;

    /**
     * Marca síncrona de "petición en curso". Un useState NO sirve para esto: al hacer varios
     * clics seguidos, React todavía no ha vuelto a renderizar y todas las llamadas leerían el
     * valor antiguo de `registrando`, enviando varias peticiones. Un ref se actualiza en el
     * momento, de modo que solo la primera pulsación pasa.
     */
    const envioEnCurso = useRef(false);

    const handleCheckIn = async () => {
        if (!reservaSeleccionada || envioEnCurso.current) return;
        envioEnCurso.current = true;
        try {
            setRegistrando(true);
            setError("");
            setMensaje("");
            const estadia: Estadia = await hotelService.checkIn(reservaSeleccionada.id);
            setMensaje(
                `Check-in realizado. Estadía ${estadia.id} registrada el ${formatoFechaHoraLocal(estadia.fechaInicio)} para ` +
                `${reservaSeleccionada.huesped?.nombre} ${reservaSeleccionada.huesped?.apellido} ` +
                `(doc. ${reservaSeleccionada.huesped?.documento}) en la habitación ${reservaSeleccionada.numeroHabitacion}.`
            );
            setReservaSeleccionada(null);
            // Se recarga desde el servidor para que la lista refleje el estado real.
            await cargar();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error realizando check-in");
        } finally {
            envioEnCurso.current = false;
            setRegistrando(false);
        }
    };

    return <div className="p-8">
        <div className="mb-8"><h1 className="text-3xl font-bold text-gray-900">Check-in</h1><p className="text-gray-500 mt-1">Registrar llegada de huéspedes</p></div>
        {error && <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">{error}</div>}
        {mensaje && <div className="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg text-green-700 text-sm">{mensaje}</div>}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card><h2 className="text-lg font-semibold text-gray-900 mb-4">Buscar Reserva</h2><div className="relative mb-4"><Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" /><input type="text" aria-label="Buscar reserva por nombre, documento, código o habitación" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} placeholder="Buscar por nombre, documento, reserva o habitación..." className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg" /></div>
                {cargando ? <p className="text-gray-500">Cargando reservas...</p>
                    : reservasFiltradas.length === 0 ? <p className="text-gray-500">No hay reservas habilitadas para check-in.</p>
                        : <div className="space-y-3">{reservasFiltradas.map((reserva) => {
                            const bloqueada = !esHabilitada(reserva);
                            return <button type="button" key={reserva.id}
                                onClick={() => { setReservaSeleccionada(reserva); setError(""); setMensaje(""); }}
                                aria-pressed={reservaSeleccionada?.id === reserva.id}
                                aria-label={`Seleccionar reserva ${reserva.codigoReserva}, habitación ${reserva.numeroHabitacion}, huésped ${reserva.huesped?.nombre ?? ""} ${reserva.huesped?.apellido ?? ""}`}
                                className={`block w-full text-left p-4 border rounded-lg cursor-pointer transition-all focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 ${reservaSeleccionada?.id === reserva.id ? "border-blue-500 bg-blue-50" : "border-gray-200 hover:border-blue-300"}`}>

                                <div className="flex justify-between">
                                    <div>
                                        <p className="font-medium text-gray-900">{reserva.huesped?.nombre} {reserva.huesped?.apellido}</p>
                                        <p className="text-sm text-gray-500">Reserva {reserva.codigoReserva} - Hab. {reserva.numeroHabitacion}</p>
                                        <p className="text-sm text-gray-500">{reserva.fechaInicio} → {reserva.fechaFin}</p>
                                        {reserva.huesped?.documento && <p className="text-sm text-gray-500">Doc. {reserva.huesped.documento}</p>}
                                        {bloqueada && <p className="text-xs font-medium text-amber-700 mt-1">No habilitada para check-in</p>}
                                    </div>
                                    <LogIn className={`w-5 h-5 ${bloqueada ? "text-gray-300" : "text-blue-600"}`} />
                                </div>
                            </button>;
                        })}</div>}
            </Card>
            <Card><h2 className="text-lg font-semibold text-gray-900 mb-4">Detalle del Check-in</h2>
                {reservaSeleccionada ? <div className="space-y-4">
                    <div><p className="text-sm text-gray-600">Huésped</p><p className="font-medium text-gray-900">{reservaSeleccionada.huesped?.nombre} {reservaSeleccionada.huesped?.apellido}</p></div>
                    <div><p className="text-sm text-gray-600">Documento de identidad</p><p className="font-medium text-gray-900">{reservaSeleccionada.huesped?.documento ?? "Sin documento"}</p></div>
                    <div><p className="text-sm text-gray-600">Habitación</p><p className="font-medium text-gray-900">{reservaSeleccionada.numeroHabitacion} ({reservaSeleccionada.habitacion?.estado ?? "sin estado"})</p></div>
                    <div><p className="text-sm text-gray-600">Fechas</p><p className="font-medium text-gray-900">{reservaSeleccionada.fechaInicio} a {reservaSeleccionada.fechaFin}</p></div>
                    <div><p className="text-sm text-gray-600">Estado de la reserva</p><p className="font-medium text-gray-900">{reservaSeleccionada.estado}</p></div>
                    {motivoBloqueo(reservaSeleccionada)
                        ? <div className="p-3 bg-yellow-50 border border-yellow-200 rounded-lg text-yellow-800 text-sm">{motivoBloqueo(reservaSeleccionada)}</div>
                        : <p className="text-xs text-gray-500">Verifique el documento del huésped antes de confirmar la llegada.</p>}
                    <Button variant="success" size="lg" onClick={handleCheckIn} disabled={registrando || !!motivoBloqueo(reservaSeleccionada)} className="w-full">
                        {registrando ? "Registrando..." : "Confirmar llegada y realizar check-in"}
                    </Button>
                </div> : <p className="text-gray-500">Seleccione una reserva para continuar</p>}
            </Card>
        </div>
    </div>;
}