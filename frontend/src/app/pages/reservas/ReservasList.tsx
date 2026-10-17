import { useEffect, useRef, useState } from "react";
import { Link } from "react-router";
import { Plus, Check, X, AlertCircle, Pencil } from "lucide-react";
import { Table } from "../../components/ui/Table";
import { Button } from "../../components/ui/Button";
import { Modal } from "../../components/ui/Modal";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { Habitacion, Reserva, EstadoReserva } from "../../types";
import { hotelService } from "../../services/hotelService";
import { fechaLocalISO } from "../../utils/fechas";

/**
 * Componente para listar y gestionar reservas del hotel. Permite visualizar todas las reservas,
 * así como confirmar, cancelar, marcar como no-show o modificar una reserva según el estado de cada una.
 * @constructor
 */
export function ReservasList() {
    const [reservas, setReservas] = useState<Reserva[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [mensaje, setMensaje] = useState("");
    const [editando, setEditando] = useState<Reserva | null>(null);

    const cargarReservas = async () => {
        try {
            setLoading(true);
            setError("");
            setReservas(await hotelService.listarReservas());
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error cargando reservas");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { cargarReservas(); }, []);

    const columns = [
        { key: "codigoReserva", label: "Código" },
        { key: "huesped", label: "Huésped", render: (r: Reserva) => `${r.huesped?.nombre ?? ""} ${r.huesped?.apellido ?? ""}` },
        { key: "numeroHabitacion", label: "Habitación" },
        { key: "fechas", label: "Fechas", render: (r: Reserva) => `${r.fechaInicio} → ${r.fechaFin}` },
        { key: "personas", label: "Personas", render: (r: Reserva) => `${r.adultos} adultos, ${r.ninos} niños` },
        { key: "estado", label: "Estado", render: (r: Reserva) => <StatusBadge status={r.estado} type="reserva" /> },
    ];

    const ejecutar = async (accion: () => Promise<Reserva>, exito: string) => {
        try {
            setError("");
            setMensaje("");
            await accion();
            setMensaje(exito);
            await cargarReservas();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error actualizando reserva");
        }
    };

    const esModificable = (r: Reserva) => r.estado === EstadoReserva.RESERVADA || r.estado === EstadoReserva.CONFIRMADA;

    return (
        <div className="p-8">
            <div className="flex items-center justify-between mb-8">
                <div><h1 className="text-3xl font-bold text-gray-900">Reservas</h1><p className="text-gray-500 mt-1">Gestión de todas las reservas del hotel</p></div>
                <Link to="/reservas/nueva"><Button variant="primary" size="lg"><div className="flex items-center gap-2"><Plus className="w-5 h-5" />Nueva Reserva</div></Button></Link>
            </div>
            {error && <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">{error}</div>}
            {mensaje && <div className="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg text-green-700 text-sm">{mensaje}</div>}
            {loading ? <p className="text-gray-500">Cargando reservas...</p> : <Table data={reservas} columns={columns} searchPlaceholder="Buscar por huésped, habitación..." actions={(reserva) => <>
                {reserva.estado === EstadoReserva.RESERVADA && <Button variant="success" size="sm" onClick={() => ejecutar(() => hotelService.confirmarReserva(reserva.id), "Reserva confirmada")}><Check className="w-4 h-4" /></Button>}
                {esModificable(reserva) && <Button variant="secondary" size="sm" onClick={() => { setError(""); setMensaje(""); setEditando(reserva); }}><Pencil className="w-4 h-4" /></Button>}
                {esModificable(reserva) && <>
                    <Button variant="danger" size="sm" onClick={() => {
                        if (confirm(`¿Cancelar la reserva ${reserva.codigoReserva}?`)) {
                            ejecutar(() => hotelService.cancelarReserva(reserva.id), "Reserva cancelada");
                        }
                    }}><X className="w-4 h-4" /></Button>
                    <Button variant="secondary" size="sm" onClick={() => ejecutar(() => hotelService.noShowReserva(reserva.id), "Reserva marcada como no-show")}><AlertCircle className="w-4 h-4" /></Button>
                </>}
            </>} />}
            {editando && <ModalEditarReserva
                reserva={editando}
                onCerrar={() => setEditando(null)}
                onGuardado={(mensaje) => { setEditando(null); setMensaje(mensaje); setError(""); cargarReservas(); }}
                onError={(mensaje) => setError(mensaje)}
            />}
        </div>
    );
}

/**
 * Formulario de modificación de una reserva (F-04). Permite cambiar las fechas de entrada y salida
 * y la habitación asignada. La disponibilidad se consulta al backend cada vez que cambian las fechas,
 * pasando el identificador de la propia reserva para que no se detecte a sí misma como conflicto.
 * @param reserva reserva que se está modificando
 * @param onCerrar callback al cerrar el formulario
 * @param onGuardado callback cuando la modificación fue exitosa
 * @param onError callback cuando el backend rechazó la modificación
 */
function ModalEditarReserva({ reserva, onCerrar, onGuardado, onError }: {
    reserva: Reserva;
    onCerrar: () => void;
    onGuardado: (mensaje: string) => void;
    onError: (mensaje: string) => void;
}) {
    const [formData, setFormData] = useState({
        fechaInicio: reserva.fechaInicio,
        fechaFin: reserva.fechaFin,
        numeroHabitacion: reserva.numeroHabitacion,
    });
    const [habitaciones, setHabitaciones] = useState<Habitacion[]>([]);
    const [cargando, setCargando] = useState(false);
    const [mensajeFechas, setMensajeFechas] = useState("");
    const [guardando, setGuardando] = useState(false);

    // Solo la petición más reciente puede actualizar la lista de habitaciones.
    const peticionEnCurso = useRef(0);

    useEffect(() => {
        const { fechaInicio, fechaFin } = formData;
        const hoy = fechaLocalISO();

        let problema = "";
        if (!fechaInicio || !fechaFin) {
            problema = "Seleccione la fecha de entrada y la de salida.";
        } else if (fechaFin <= fechaInicio) {
            problema = "La fecha de salida debe ser posterior a la fecha de entrada.";
        } else if (fechaInicio < hoy) {
            problema = "La fecha de entrada no puede estar en el pasado.";
        }
        setMensajeFechas(problema);

        const numeroPeticion = ++peticionEnCurso.current;
        if (problema) {
            setHabitaciones([]);
            setCargando(false);
            return;
        }

        setCargando(true);
        hotelService.listarHabitacionesDisponibles(fechaInicio, fechaFin, reserva.id)
            .then((lista) => {
                if (numeroPeticion !== peticionEnCurso.current) return;
                setHabitaciones(lista);
                // Si la habitación elegida ya no está disponible, se limpia la selección.
                setFormData((prev) => (lista.some((h) => h.numero === prev.numeroHabitacion)
                    ? prev
                    : { ...prev, numeroHabitacion: "" }));
            })
            .catch((err) => {
                if (numeroPeticion !== peticionEnCurso.current) return;
                setHabitaciones([]);
                onError(err instanceof Error ? err.message : "Error consultando disponibilidad");
            })
            .finally(() => {
                if (numeroPeticion !== peticionEnCurso.current) return;
                setCargando(false);
            });
    }, [formData.fechaInicio, formData.fechaFin]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (mensajeFechas) return;
        try {
            setGuardando(true);
            onError("");
            await hotelService.modificarReserva(reserva.id, {
                numeroHabitacion: Number(formData.numeroHabitacion),
                fechaInicio: formData.fechaInicio,
                fechaFin: formData.fechaFin,
            });
            onGuardado(`Reserva ${reserva.codigoReserva} actualizada`);
        } catch (err) {
            onError(err instanceof Error ? err.message : "Error modificando reserva");
        } finally {
            setGuardando(false);
        }
    };

    const habitacionActual = reserva.habitacion;
    const puedeElegir = !mensajeFechas && !cargando;

    return <Modal isOpen={true} onClose={onCerrar} title={`Modificar reserva ${reserva.codigoReserva}`}>
        <form onSubmit={handleSubmit} className="space-y-4">
            <div className="bg-blue-50 p-4 rounded-lg space-y-1 text-sm">
                <div className="flex justify-between"><span>Huésped:</span><span>{reserva.huesped?.nombre} {reserva.huesped?.apellido}</span></div>
                <div className="flex justify-between"><span>Personas:</span><span>{reserva.adultos} adultos, {reserva.ninos} niños</span></div>
                <div className="flex justify-between"><span>Habitación actual:</span><span>{habitacionActual ? `Hab. ${habitacionActual.numero} - ${habitacionActual.tipo}` : reserva.numeroHabitacion}</span></div>
            </div>
            <div className="grid grid-cols-2 gap-4">
                <div><label htmlFor="editar-fecha-inicio" className="block text-sm font-medium text-gray-700 mb-2">Fecha inicio *</label><input id="editar-fecha-inicio" type="date" value={formData.fechaInicio} onChange={(e) => setFormData({ ...formData, fechaInicio: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required /></div>
                <div><label htmlFor="editar-fecha-fin" className="block text-sm font-medium text-gray-700 mb-2">Fecha fin *</label><input id="editar-fecha-fin" type="date" value={formData.fechaFin} onChange={(e) => setFormData({ ...formData, fechaFin: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required /></div>
            </div>
            <div>
                <label htmlFor="editar-habitacion" className="block text-sm font-medium text-gray-700 mb-2">Habitación *</label>
                <select id="editar-habitacion" value={formData.numeroHabitacion} disabled={!puedeElegir} onChange={(e) => setFormData({ ...formData, numeroHabitacion: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg disabled:bg-gray-100 disabled:text-gray-400" required>
                    <option value="">{cargando ? "Consultando disponibilidad..." : "Seleccione una habitación..."}</option>
                    {habitaciones.map((h) => <option key={h.id} value={h.numero}>Hab. {h.numero} - {h.tipo} - ${h.precioNoche}</option>)}
                </select>
                <p className="text-xs text-gray-500 mt-1">
                    {mensajeFechas ? mensajeFechas
                        : habitaciones.length === 0 && !cargando ? "No hay habitaciones disponibles para las fechas seleccionadas."
                            : "La habitación actual se muestra como disponible porque no se considera en conflicto consigo misma."}
                </p>
            </div>
            <div className="flex justify-end gap-3 pt-2">
                <Button type="button" variant="secondary" onClick={onCerrar}>Cancelar</Button>
                <Button type="submit" variant="primary" disabled={guardando || !!mensajeFechas || !formData.numeroHabitacion}>
                    {guardando ? "Guardando..." : "Guardar cambios"}
                </Button>
            </div>
        </form>
    </Modal>;
}