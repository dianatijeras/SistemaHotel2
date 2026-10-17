import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { ArrowLeft } from "lucide-react";
import { Card } from "../../components/ui/Card";
import { Button } from "../../components/ui/Button";
import { Habitacion, Huesped } from "../../types";
import { hotelService } from "../../services/hotelService";
import { fechaLocalISO } from "../../utils/fechas";

/**
 * Componente para crear una nueva reserva en el hotel. Permite seleccionar un huésped, una habitación disponible, fechas de estadía y número de personas para generar una nueva reserva.
 * La disponibilidad de habitaciones se consulta al backend según el intervalo de fechas escolhido: primero se piden las fechas y solo después se cargan las habitaciones libres para ese periodo.
 * @constructor
 */
export function ReservasNueva() {
    const navigate = useNavigate();
    const [huespedes, setHuespedes] = useState<Huesped[]>([]);
    const [habitacionesDisponibles, setHabitacionesDisponibles] = useState<Habitacion[]>([]);
    const [error, setError] = useState("");
    const [mensajeFechas, setMensajeFechas] = useState("");
    const [consultado, setConsultado] = useState(false);
    const [cargandoDisponibilidad, setCargandoDisponibilidad] = useState(false);
    const [refrescar, setRefrescar] = useState(0);
    const [formData, setFormData] = useState({ idHuesped: "", numeroHabitacion: "", fechaInicio: "", fechaFin: "", adultos: 1, ninos: 0 });

    /** Identificador de la petición en curso. Solo la respuesta más reciente puede actualizar la lista,
     *  de modo que una respuesta antigua no sobrescribe el resultado de unas fechas más nuevas. */
    const peticionEnCurso = useRef(0);

    const hoy = fechaLocalISO();

    useEffect(() => {
        hotelService.listarHuespedes()
            .then(setHuespedes)
            .catch((err) => setError(err instanceof Error ? err.message : "Error cargando huéspedes"));
    }, []);

    /**
     * Verifica las fechas escritas aplicando las mismas reglas que usa el backend, para no
     * consultar disponibilidad con un intervalo que el servidor va a rechazar.
     * @param inicio fecha de entrada en formato AAAA-MM-DD
     * @param fin fecha de salida en formato AAAA-MM-DD
     * @returns un mensaje si las fechas no permiten consultar, o una cadena vacía si son válidas
     */
    const validarFechas = (inicio: string, fin: string): string => {
        if (!inicio || !fin) {
            return "Seleccione la fecha de entrada y la de salida para consultar la disponibilidad.";
        }
        if (fin <= inicio) {
            return "La fecha de salida debe ser posterior a la fecha de entrada.";
        }
        if (inicio < hoy) {
            return "La fecha de entrada no puede estar en el pasado.";
        }
        return "";
    };

    useEffect(() => {
        const { fechaInicio, fechaFin } = formData;
        const problema = validarFechas(fechaInicio, fechaFin);
        const numeroPeticion = ++peticionEnCurso.current;

        setMensajeFechas(problema);
        setConsultado(false);

        // Sin fechas válidas no se consulta nada y la habitación elegida deja de estar seleccionada.
        if (problema) {
            setHabitacionesDisponibles([]);
            setCargandoDisponibilidad(false);
            setFormData((prev) => (prev.numeroHabitacion ? { ...prev, numeroHabitacion: "" } : prev));
            return;
        }

        setCargandoDisponibilidad(true);
        hotelService.listarHabitacionesDisponibles(fechaInicio, fechaFin)
            .then((habitaciones) => {
                if (numeroPeticion !== peticionEnCurso.current) return;
                setHabitacionesDisponibles(habitaciones);
                setConsultado(true);
                // Si la habitación seleccionada ya no está disponible para las fechas nuevas, se limpia.
                setFormData((prev) => {
                    if (!prev.numeroHabitacion) return prev;
                    return habitaciones.some((h) => h.numero === prev.numeroHabitacion)
                        ? prev
                        : { ...prev, numeroHabitacion: "" };
                });
            })
            .catch((err) => {
                if (numeroPeticion !== peticionEnCurso.current) return;
                setHabitacionesDisponibles([]);
                setConsultado(true);
                setError(err instanceof Error ? err.message : "Error consultando disponibilidad");
            })
            .finally(() => {
                if (numeroPeticion !== peticionEnCurso.current) return;
                setCargandoDisponibilidad(false);
            });
    }, [formData.fechaInicio, formData.fechaFin, refrescar]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            setError("");
            await hotelService.crearReserva({ ...formData, numeroHabitacion: Number(formData.numeroHabitacion) });
            navigate("/reservas");
        } catch (err) {
            // El backend rechaza la reserva si la habitación se acaba de ocupar: se muestra su
            // mensaje y se vuelve a consultar la disponibilidad sin crear una reserva duplicada.
            setError(err instanceof Error ? err.message : "Error creando reserva");
            setRefrescar((n) => n + 1);
        }
    };

    // Solo se puede elegir habitación cuando hay fechas váliles y el backend ya respondió.
    const puedeElegirHabitacion = !mensajeFechas && !cargandoDisponibilidad && consultado;

    return <div className="p-8">
        <Button variant="secondary" size="sm" onClick={() => navigate(-1)}><ArrowLeft className="w-4 h-4 mr-2" />Volver</Button>
        <div className="mt-4 mb-8"><h1 className="text-3xl font-bold text-gray-900">Nueva Reserva</h1><p className="text-gray-500 mt-1">Crear una nueva reserva</p></div>
        {error && <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">{error}</div>}
        <Card><form onSubmit={handleSubmit} className="space-y-6">
            <div className="grid grid-cols-2 gap-4">
                <div><label htmlFor="nueva-fecha-inicio" className="block text-sm font-medium text-gray-700 mb-2">Fecha inicio *</label><input id="nueva-fecha-inicio" type="date" min={hoy} value={formData.fechaInicio} onChange={(e) => setFormData({ ...formData, fechaInicio: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required /></div>
                <div><label htmlFor="nueva-fecha-fin" className="block text-sm font-medium text-gray-700 mb-2">Fecha fin *</label><input id="nueva-fecha-fin" type="date" min={hoy} value={formData.fechaFin} onChange={(e) => setFormData({ ...formData, fechaFin: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required /></div>
            </div>
            <div className="grid grid-cols-2 gap-4">
                <div><label htmlFor="nueva-huesped" className="block text-sm font-medium text-gray-700 mb-2">Huésped *</label><select id="nueva-huesped" value={formData.idHuesped} onChange={(e) => setFormData({ ...formData, idHuesped: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required><option value="">Seleccione...</option>{huespedes.map((h) => <option key={h.id} value={h.id}>{h.nombre} {h.apellido}</option>)}</select></div>
                <div>
                    <label htmlFor="nueva-habitacion" className="block text-sm font-medium text-gray-700 mb-2">Habitación *</label>
                    <select id="nueva-habitacion" value={formData.numeroHabitacion} disabled={!puedeElegirHabitacion} onChange={(e) => setFormData({ ...formData, numeroHabitacion: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg disabled:bg-gray-100 disabled:text-gray-400" required>
                        <option value="">{mensajeFechas || cargandoDisponibilidad || !consultado ? "Seleccione las fechas..." : "Seleccione..."}</option>
                        {habitacionesDisponibles.map((h) => <option key={h.id} value={h.numero}>Hab. {h.numero} - {h.tipo} - ${h.precioNoche}</option>)}
                    </select>
                    <p className="text-xs text-gray-500 mt-1">
                        {cargandoDisponibilidad
                            ? "Consultando disponibilidad..."
                            : mensajeFechas
                                ? mensajeFechas
                                : consultado && habitacionesDisponibles.length === 0
                                    ? "No hay habitaciones disponibles para las fechas seleccionadas."
                                    : `${habitacionesDisponibles.length} habitación(es) disponible(s) para las fechas seleccionadas.`}
                    </p>
                </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
                <div><label htmlFor="nueva-adultos" className="block text-sm font-medium text-gray-700 mb-2">Adultos *</label><input id="nueva-adultos" type="number" min="1" value={formData.adultos} onChange={(e) => setFormData({ ...formData, adultos: Number(e.target.value) })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required /></div>
                <div><label htmlFor="nueva-ninos" className="block text-sm font-medium text-gray-700 mb-2">Niños</label><input id="nueva-ninos" type="number" min="0" value={formData.ninos} onChange={(e) => setFormData({ ...formData, ninos: Number(e.target.value) })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" /></div>
            </div>
            <div className="flex justify-end gap-3"><Button type="button" variant="secondary" onClick={() => navigate("/reservas")}>Cancelar</Button><Button type="submit" variant="primary">Crear Reserva</Button></div>
        </form></Card>
    </div>;
}