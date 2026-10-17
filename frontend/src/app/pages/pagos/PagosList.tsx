import { useEffect, useRef, useState } from "react";
import { Plus, Check, X, RotateCcw } from "lucide-react";
import { Table } from "../../components/ui/Table";
import { Button } from "../../components/ui/Button";
import { Modal } from "../../components/ui/Modal";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { EstadoPago, Estadia, MetodoPago, Pago, SaldoPago } from "../../types";
import { formatMoneda, hotelService } from "../../services/hotelService";

const FORMULARIO_VACIO = {
    idEstadia: "",
    monto: 0,
    metodoPago: MetodoPago.EFECTIVO,
    concepto: "",
};

/**
 * Componente para listar y gestionar pagos del hotel (F-10).
 *
 * Permite registrar pagos parciales o totales, aprobarlos o rechazarlos con acciones explícitas
 * (simulación interna, sin pasarela real) y seguir el saldo pendiente de cada estadía.
 * Todos los importes y el saldo los calcula el backend: aquí no se multiplica ni se resta nada.
 * @constructor
 */
export function PagosList() {
    const [pagos, setPagos] = useState<Pago[]>([]);
    const [estadias, setEstadias] = useState<Estadia[]>([]);
    const [saldos, setSaldos] = useState<Record<string, SaldoPago>>({});
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [mensaje, setMensaje] = useState("");
    const [enviando, setEnviando] = useState(false);
    // Marca síncrona de envío: un useState no sirve, porque al pulsar dos veces seguidas React
    // todavía no ha vuelto a renderizar y ambas llamadas pasarían el guardia.
    const envioEnCurso = useRef(false);
    const [formData, setFormData] = useState({ ...FORMULARIO_VACIO });

    const cargar = async () => {
        try {
            setLoading(true);
            setError("");
            const [pagosData, estadiasData] = await Promise.all([
                hotelService.listarPagos(),
                hotelService.listarEstadiasParaPago(),
            ]);
            setPagos(pagosData);
            setEstadias(estadiasData);

            // El saldo de cada estadía se pide al backend, que es su única fuente de verdad.
            const saldosData: Record<string, SaldoPago> = {};
            for (const estadia of estadiasData) {
                saldosData[estadia.id] = await hotelService.consultarSaldo(estadia.id);
            }
            setSaldos(saldosData);
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error cargando pagos");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { cargar(); }, []);

    const columns = [
        { key: "id", label: "ID" },
        { key: "idEstadia", label: "Estadía" },
        { key: "concepto", label: "Concepto" },
        { key: "monto", label: "Monto", render: (p: Pago) => formatMoneda(p.monto) },
        { key: "metodoPago", label: "Método" },
        { key: "estado", label: "Estado", render: (p: Pago) => <StatusBadge status={p.estado} type="pago" /> },
    ];

    /** Al elegir una estadía se propone el saldo exacto, que el usuario puede ajustar (pago parcial). */
    const handleSeleccionarEstadia = (idEstadia: string) => {
        const saldo = saldos[idEstadia];
        setFormData({
            idEstadia,
            monto: saldo ? Number(saldo.saldoPendiente.toFixed(2)) : 0,
            metodoPago: MetodoPago.EFECTIVO,
            concepto: "",
        });
        setError("");
    };

    const estadiaSeleccionada = estadias.find((e) => e.id === formData.idEstadia);
    const saldoSeleccionado = formData.idEstadia ? saldos[formData.idEstadia] : undefined;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!formData.idEstadia) {
            setError("Debe seleccionar una estadía para registrar el pago.");
            return;
        }
        if (!(formData.monto > 0)) {
            setError("El monto del pago debe ser mayor a 0.");
            return;
        }
        if (!formData.concepto.trim()) {
            setError("Debe indicar el concepto del pago.");
            return;
        }
        if (envioEnCurso.current) return;
        envioEnCurso.current = true;
        try {
            setEnviando(true);
            setError("");
            setMensaje("");
            await hotelService.crearPago({
                idEstadia: formData.idEstadia,
                monto: Number(formData.monto),
                metodoPago: formData.metodoPago,
                concepto: formData.concepto.trim(),
            });
            setIsModalOpen(false);
            setFormData({ ...FORMULARIO_VACIO });
            setMensaje("Pago registrado en estado PENDIENTE. Apruébalo para aplicarlo al saldo.");
            await cargar();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error registrando pago");
        } finally {
            envioEnCurso.current = false;
            setEnviando(false);
        }
    };

    const ejecutar = async (accion: () => Promise<Pago>, exito: string) => {
        if (envioEnCurso.current) return;
        envioEnCurso.current = true;
        try {
            setEnviando(true);
            setError("");
            setMensaje("");
            await accion();
            setMensaje(exito);
            await cargar();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error actualizando pago");
        } finally {
            envioEnCurso.current = false;
            setEnviando(false);
        }
    };

    return <div className="p-8">
        <div className="flex items-center justify-between mb-8"><div><h1 className="text-3xl font-bold text-gray-900">Pagos</h1><p className="text-gray-500 mt-1">Gestión de pagos y cobros</p></div><Button variant="primary" size="lg" onClick={() => { setIsModalOpen(true); setError(""); setMensaje(""); }} disabled={enviando}><div className="flex items-center gap-2"><Plus className="w-5 h-5" />Nuevo Pago</div></Button></div>
        {error && <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">{error}</div>}
        {mensaje && <div className="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg text-green-700 text-sm">{mensaje}</div>}

        {estadias.length > 0 && <div className="mb-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {estadias.map((e) => {
                const saldo = saldos[e.id];
                const pagado = saldo ? Number(saldo.saldoPendiente) === 0 : false;
                return <div key={e.id} className={`p-4 rounded-lg border ${pagado ? "border-green-200 bg-green-50" : "border-gray-200 bg-white"}`}>
                    <p className="font-medium text-gray-900">{e.reserva?.huesped?.nombre} {e.reserva?.huesped?.apellido}</p>
                    <p className="text-sm text-gray-500">Hab. {e.reserva?.numeroHabitacion} · Estadía {e.id}</p>
                    {saldo && <div className="mt-3 space-y-1 text-sm">
                        <div className="flex justify-between"><span className="text-gray-600">Total:</span><span>{formatMoneda(saldo.totalGeneral)}</span></div>
                        <div className="flex justify-between"><span className="text-gray-600">Aprobado:</span><span>{formatMoneda(saldo.totalAprobado)}</span></div>
                        <div className="flex justify-between font-semibold"><span>Saldo:</span><span className={pagado ? "text-green-700" : "text-red-700"}>{pagado ? "Pagado" : formatMoneda(saldo.saldoPendiente)}</span></div>
                    </div>}
                </div>;
            })}
        </div>}

        {loading ? <p className="text-gray-500">Cargando pagos...</p> : <Table data={pagos} columns={columns} searchPlaceholder="Buscar pago..." actions={(pago) => <>
            {pago.estado === EstadoPago.PENDIENTE && <><Button variant="success" size="sm" title="Aprobar pago" disabled={enviando} onClick={() => ejecutar(() => hotelService.aprobarPago(pago.id, "REF-MANUAL"), `Pago ${pago.id} aprobado. El saldo se actualizó.`)}><Check className="w-4 h-4" /></Button><Button variant="danger" size="sm" title="Rechazar pago" disabled={enviando} onClick={() => ejecutar(() => hotelService.rechazarPago(pago.id, "Rechazado manualmente"), `Pago ${pago.id} rechazado. El saldo no cambia.`)}><X className="w-4 h-4" /></Button></>}
            {pago.estado === EstadoPago.PAGADO && <Button variant="secondary" size="sm" title="Reembolsar pago" disabled={enviando} onClick={() => ejecutar(() => hotelService.reembolsarPago(pago.id), `Pago ${pago.id} reembolsado.`)}><RotateCcw className="w-4 h-4" /></Button>}
        </>} />}

        <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title="Nuevo Pago"><form onSubmit={handleSubmit} className="space-y-4">
            <div><label htmlFor="pago-estadia" className="block text-sm font-medium text-gray-700 mb-2">Estadía *</label><select id="pago-estadia" value={formData.idEstadia} onChange={(e) => handleSeleccionarEstadia(e.target.value)} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required><option value="">Seleccione estadía</option>{estadias.map((e) => <option key={e.id} value={e.id}>{e.id} - {e.reserva?.huesped?.nombre ?? "Huésped"} {e.reserva?.huesped?.apellido ?? ""} - {e.estado}</option>)}</select>{estadias.length === 0 && <p className="text-xs text-gray-500 mt-2">No hay estadías en curso o finalizadas disponibles para pago. Primero realiza el check-in de una reserva.</p>}</div>

            {saldoSeleccionado && <div className="bg-blue-50 p-4 rounded-lg space-y-2 text-sm">
                <div className="flex justify-between"><span>Total:</span><span>{formatMoneda(saldoSeleccionado.totalGeneral)}</span></div>
                <div className="flex justify-between"><span>Ya aprobado:</span><span>{formatMoneda(saldoSeleccionado.totalAprobado)}</span></div>
                <div className="flex justify-between"><span>Pendientes por decidir:</span><span>{saldoSeleccionado.cantidadPagosPendientes}</span></div>
                <div className="border-t border-blue-200 pt-2 flex justify-between font-bold"><span>Saldo a cobrar:</span><span>{formatMoneda(saldoSeleccionado.saldoPendiente)}</span></div>
            </div>}

            <div><label htmlFor="pago-monto" className="block text-sm font-medium text-gray-700 mb-2">Monto *</label><input id="pago-monto" type="number" min="0.01" step="0.01" value={formData.monto} onChange={(e) => setFormData({ ...formData, monto: Number(e.target.value) })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required /><p className="text-xs text-gray-500 mt-1">Puedes registrar un pago parcial. El pago se guarda PENDIENTE y solo descuenta del saldo cuando lo apruebes.</p></div>

            <div><label htmlFor="pago-concepto" className="block text-sm font-medium text-gray-700 mb-2">Concepto *</label><input id="pago-concepto" type="text" maxLength={200} value={formData.concepto} onChange={(e) => setFormData({ ...formData, concepto: e.target.value })} placeholder="Ej: Abono a la noche del 15" className="w-full px-4 py-2 border border-gray-300 rounded-lg" required /></div>

            <div><label htmlFor="pago-metodo" className="block text-sm font-medium text-gray-700 mb-2">Método *</label><select id="pago-metodo" value={formData.metodoPago} onChange={(e) => setFormData({ ...formData, metodoPago: e.target.value as MetodoPago })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required>{Object.values(MetodoPago).map((m) => <option key={m} value={m}>{m}</option>)}</select><p className="text-xs text-gray-500 mt-1">No se solicitan datos de tarjeta: el sistema no se conecta a ninguna pasarela real.</p></div>

            <div className="flex justify-end gap-3"><Button type="button" variant="secondary" onClick={() => setIsModalOpen(false)}>Cancelar</Button><Button type="submit" variant="primary" disabled={enviando}>{enviando ? "Registrando..." : "Registrar pago"}</Button></div>
        </form></Modal>
    </div>;
}