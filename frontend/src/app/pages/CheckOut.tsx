import { useEffect, useRef, useState } from "react";
import { Search, LogOut, Eye } from "lucide-react";
import { Link } from "react-router";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Estadia, SaldoPago } from "../types";
import { formatMoneda, hotelService } from "../services/hotelService";

/**
 * Página de Check-out del hotel (F-08).
 *
 * Permite localizar una estadía activa, revisar la cuenta completa (total, pagos aprobados y
 * saldo pendiente) y registrar la salida del huésped. El saldo NO se calcula aquí: se pide al
 * backend, que es su única fuente de verdad, igual que el total de F-09 y el saldo de F-10.
 *
 * El botón de finalizar queda deshabilitado mientras quede dinero por cobrar, pero el backend
 * vuelve a comprobarlo igualmente: esta pantalla solo evita un error evidente, no es la regla.
 * @constructor
 */
export function CheckOut() {
    const [busqueda, setBusqueda] = useState("");
    const [estadias, setEstadias] = useState<Estadia[]>([]);
    const [saldos, setSaldos] = useState<Record<string, SaldoPago>>({});
    const [estadiaSeleccionada, setEstadiaSeleccionada] = useState<Estadia | null>(null);
    const [error, setError] = useState("");
    const [mensaje, setMensaje] = useState("");
    const [enviando, setEnviando] = useState(false);
    // Marca síncrona de envío: un useState no sirve porque al pulsar dos veces seguidas React
    // todavía no ha vuelto a renderizar y ambas llamadas pasarían el guardia.
    const envioEnCurso = useRef(false);

    const cargar = async () => {
        try {
            setError("");
            const lista = await hotelService.listarEstadiasEnCurso();
            setEstadias(lista);

            const saldosData: Record<string, SaldoPago> = {};
            for (const estadia of lista) {
                saldosData[estadia.id] = await hotelService.consultarSaldo(estadia.id);
            }
            setSaldos(saldosData);
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error cargando estadías");
        }
    };
    useEffect(() => { cargar(); }, []);

    const estadiasFiltradas = estadias.filter((e) => `${e.id} ${e.reserva?.huesped?.nombre ?? ""} ${e.reserva?.huesped?.apellido ?? ""} ${e.reserva?.numeroHabitacion ?? ""}`.toLowerCase().includes(busqueda.toLowerCase()));

    const saldoSeleccionado = estadiaSeleccionada ? saldos[estadiaSeleccionada.id] : undefined;
    const saldoCerrado = saldoSeleccionado ? Number(saldoSeleccionado.saldoPendiente) === 0 : false;

    const seleccionar = (estadia: Estadia) => {
        setEstadiaSeleccionada(estadia);
        setError("");
        setMensaje("");
    };

    const handleCheckOut = async () => {
        if (!estadiaSeleccionada || envioEnCurso.current) return;
        envioEnCurso.current = true;
        try {
            setEnviando(true);
            setError("");
            setMensaje("");
            await hotelService.checkOut(estadiaSeleccionada.id);
            setMensaje("Check-out realizado. La habitación quedó EN_LIMPIEZA hasta confirmar la limpieza.");
            setEstadiaSeleccionada(null);
            await cargar();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error realizando check-out");
        } finally {
            envioEnCurso.current = false;
            setEnviando(false);
        }
    };

    return <div className="p-8">
        <div className="mb-8"><h1 className="text-3xl font-bold text-gray-900">Check-out</h1><p className="text-gray-500 mt-1">Finalizar estadía y cerrar la cuenta</p></div>
        {error && <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">{error}</div>}
        {mensaje && <div className="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg text-green-700 text-sm">{mensaje}</div>}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card><h2 className="text-lg font-semibold text-gray-900 mb-4">Estadías Activas</h2><div className="relative mb-4"><Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" /><input type="text" aria-label="Buscar estadía por huésped o habitación" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} placeholder="Buscar por huésped o habitación..." className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg" /></div>
                {estadias.length === 0 && <p className="text-gray-500 text-sm">No hay estadías activas. Primero realiza el check-in de una reserva.</p>}
                <div className="space-y-3">{estadiasFiltradas.map((estadia) => {
                    const saldo = saldos[estadia.id];
                    const pagada = saldo ? Number(saldo.saldoPendiente) === 0 : false;
                    return <div key={estadia.id} onClick={() => seleccionar(estadia)} className={`p-4 border rounded-lg cursor-pointer transition-all ${estadiaSeleccionada?.id === estadia.id ? "border-blue-500 bg-blue-50" : "border-gray-200 hover:border-blue-300"}`}>
                        <div className="flex justify-between">
                            <div>
                                <p className="font-medium text-gray-900">{estadia.reserva?.huesped?.nombre} {estadia.reserva?.huesped?.apellido}</p>
                                <p className="text-sm text-gray-500">Estadía {estadia.id} - Hab. {estadia.reserva?.numeroHabitacion}</p>
                                {saldo && <p className={`text-sm font-medium ${pagada ? "text-green-700" : "text-red-700"}`}>
                                    Saldo: {pagada ? "Pagado" : formatMoneda(saldo.saldoPendiente)}
                                </p>}
                            </div>
                            <LogOut className="w-5 h-5 text-blue-600" />
                        </div>
                    </div>;
                })}</div>
            </Card>
            <Card><h2 className="text-lg font-semibold text-gray-900 mb-4">Resumen de Check-out</h2>{estadiaSeleccionada ? <div className="space-y-4">
                <div><p className="text-sm text-gray-600">Huésped</p><p className="font-medium text-gray-900">{estadiaSeleccionada.reserva?.huesped?.nombre} {estadiaSeleccionada.reserva?.huesped?.apellido}</p></div>
                <div><p className="text-sm text-gray-600">Habitación</p><p className="font-medium text-gray-900">{estadiaSeleccionada.reserva?.numeroHabitacion}</p></div>

                {saldoSeleccionado && <div className={`p-4 rounded-lg space-y-2 text-sm ${saldoCerrado ? "bg-green-50" : "bg-amber-50"}`}>
                    <div className="flex justify-between"><span>Total de la cuenta:</span><span>{formatMoneda(saldoSeleccionado.totalGeneral)}</span></div>
                    <div className="flex justify-between"><span>Pagos aprobados:</span><span>{formatMoneda(saldoSeleccionado.totalAprobado)}</span></div>
                    <div className="flex justify-between"><span>Pagos pendientes de decidir:</span><span>{saldoSeleccionado.cantidadPagosPendientes}</span></div>
                    <div className={`flex justify-between font-bold ${saldoCerrado ? "" : "text-red-700"}`}>
                        <span>Saldo pendiente:</span>
                        <span>{saldoCerrado ? "Pagado" : formatMoneda(saldoSeleccionado.saldoPendiente)}</span>
                    </div>
                </div>}

                {!saldoCerrado && <div className="p-3 bg-yellow-50 border border-yellow-200 rounded-lg text-yellow-800 text-sm">
                    Todavía hay dinero por cobrar. Registra y aprueba los pagos en el módulo de Pagos antes de finalizar el check-out.
                </div>}

                <div className="flex gap-3">
                    <Link to="/pagos" className="flex-1"><Button variant="secondary" className="w-full" disabled={enviando}>Ir a Pagos</Button></Link>
                    <Link to={`/estadias/${estadiaSeleccionada.id}`} className="flex-1"><Button variant="secondary" className="w-full" disabled={enviando}><Eye className="w-4 h-4 mr-2" />Ver Detalle</Button></Link>
                    <Button variant="primary" onClick={handleCheckOut} disabled={enviando || !saldoCerrado} className="flex-1">
                        {enviando ? "Finalizando..." : "Finalizar"}
                    </Button>
                </div>
            </div> : <p className="text-gray-500">Seleccione una estadía para continuar</p>}</Card>
        </div>
    </div>;
}