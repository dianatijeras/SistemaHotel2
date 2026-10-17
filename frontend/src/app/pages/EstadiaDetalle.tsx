import { useEffect, useRef, useState } from "react";
import { useParams, useNavigate } from "react-router";
import { ArrowLeft, Plus } from "lucide-react";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Modal } from "../components/ui/Modal";
import { Table } from "../components/ui/Table";
import { Estadia, EstadiaCosto } from "../types";
import { formatMoneda, hotelService } from "../services/hotelService";
import { formatoFechaHoraLocal } from "../utils/formatoFechaHora";

/**
 * Componente para mostrar el detalle de una estadía, incluyendo información general, consumos
 * asociados y el desglose del costo total (F-09).
 *
 * Todos los importes provienen del desglose que calcula el backend (GET /estadias/{id}/costo):
 * esta pantalla no multiplica noches por precio ni suma consumos por su cuenta, para que la
 * recepción vea exactamente las mismas cifras que se registran en el sistema.
 * @constructor
 */
export function EstadiaDetalle() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [estadia, setEstadia] = useState<Estadia | null>(null);
    const [costo, setCosto] = useState<EstadiaCosto | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [error, setError] = useState("");
    const [enviando, setEnviando] = useState(false);
    const envioEnCurso = useRef(false);
    const [formData, setFormData] = useState({ descripcion: "", cantidad: 1, precioUnitario: 0 });

    const cargar = async () => {
        if (!id) return;
        try {
            setError("");
            // Estadía y desglose se piden juntos: si el cálculo falla, se muestra el error del
            // backend en lugar de mostrar totales incompletos.
            const [detalle, desglose] = await Promise.all([
                hotelService.buscarEstadia(id),
                hotelService.obtenerCostoEstadia(id),
            ]);
            setEstadia(detalle);
            setCosto(desglose);
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error cargando estadía");
        }
    };
    useEffect(() => { cargar(); }, [id]);

    const handleAgregarConsumo = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!estadia || envioEnCurso.current) return;
        envioEnCurso.current = true;
        try {
            setEnviando(true);
            setError("");
            await hotelService.agregarConsumo({ idEstadia: estadia.id, ...formData });
            setIsModalOpen(false);
            setFormData({ descripcion: "", cantidad: 1, precioUnitario: 0 });
            // Se recarga para que el desglose refleje el total ya actualizado por el backend.
            await cargar();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error agregando consumo");
        } finally {
            envioEnCurso.current = false;
            setEnviando(false);
        }
    };

    const columns = [
        { key: "descripcion", label: "Descripción" },
        { key: "cantidad", label: "Cantidad" },
        { key: "precioUnitario", label: "Precio Unit.", render: (c: { precioUnitario: number }) => formatMoneda(c.precioUnitario) },
        { key: "subtotal", label: "Subtotal", render: (c: { subtotal: number }) => formatMoneda(c.subtotal) },
    ];

    if (!estadia) return <div className="p-8"><Button variant="secondary" size="sm" onClick={() => navigate(-1)}><ArrowLeft className="w-4 h-4 mr-2" />Volver</Button>{error ? <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">{error}</div> : <p className="mt-4 text-gray-500">Cargando estadía...</p>}</div>;

    const consumos = costo?.consumos ?? [];
    return <div className="p-8">
        <div className="mb-8"><Button variant="secondary" size="sm" onClick={() => navigate(-1)}><ArrowLeft className="w-4 h-4 mr-2" />Volver</Button><h1 className="text-3xl font-bold text-gray-900 mt-4">Estadía #{estadia.id}</h1><p className="text-gray-500 mt-1">Detalle, consumos y costo total</p></div>
        {error && <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">{error}</div>}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6"><div className="lg:col-span-2 space-y-6"><Card><h2 className="text-lg font-semibold text-gray-900 mb-4">Información General</h2><div className="grid grid-cols-2 gap-4"><div><p className="text-sm text-gray-600">Huésped</p><p className="font-medium text-gray-900">{estadia.reserva?.huesped?.nombre} {estadia.reserva?.huesped?.apellido}</p></div><div><p className="text-sm text-gray-600">Habitación</p><p className="font-medium text-gray-900">{estadia.reserva?.numeroHabitacion}</p></div><div><p className="text-sm text-gray-600">Check-in</p><p className="font-medium text-gray-900">{formatoFechaHoraLocal(estadia.fechaInicio)}</p></div><div><p className="text-sm text-gray-600">Estado</p><span className="px-2 py-1 text-xs font-medium rounded-full bg-green-100 text-green-800">{estadia.estado}</span></div></div></Card><Card><div className="flex items-center justify-between mb-4"><h2 className="text-lg font-semibold text-gray-900">Consumos</h2><Button variant="primary" onClick={() => setIsModalOpen(true)}><Plus className="w-4 h-4 mr-2" />Agregar Consumo</Button></div>{consumos.length === 0 ? <p className="text-gray-500 text-sm">La estadía no tiene consumos registrados.</p> : <Table data={consumos} columns={columns} />}</Card></div><div><Card className="sticky top-8"><h2 className="text-lg font-semibold text-gray-900 mb-4">Resumen de Cobro</h2>{costo ? <div className="space-y-3">
            <div className="flex justify-between text-sm"><span className="text-gray-600">Noches:</span><span className="font-medium text-gray-900">{costo.noches}</span></div>
            <div className="flex justify-between text-sm"><span className="text-gray-600">Precio por noche:</span><span className="font-medium text-gray-900">{formatMoneda(costo.precioNoche)}</span></div>
            <div className="flex justify-between text-sm border-b border-gray-200 pb-3"><span className="text-gray-600">Subtotal alojamiento:</span><span className="font-medium text-gray-900">{formatMoneda(costo.subtotalAlojamiento)}</span></div>
            <div className="flex justify-between text-sm"><span className="text-gray-600">Subtotal consumos ({consumos.length}):</span><span className="font-medium text-gray-900">{formatMoneda(costo.subtotalConsumos)}</span></div>
            <div className="border-t pt-3 flex justify-between"><span className="font-semibold">Total general:</span><span className="text-xl font-bold text-blue-600">{formatMoneda(costo.totalGeneral)}</span></div>
        </div> : <p className="text-gray-500 text-sm">Costo no disponible.</p>}</Card></div></div>
        <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title="Agregar Consumo"><form onSubmit={handleAgregarConsumo} className="space-y-4"><div><label htmlFor="consumo-descripcion" className="block text-sm font-medium text-gray-700 mb-2">Descripción *</label><input id="consumo-descripcion" value={formData.descripcion} onChange={(e) => setFormData({ ...formData, descripcion: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required /></div><div className="grid grid-cols-2 gap-4"><div><label htmlFor="consumo-cantidad" className="block text-sm font-medium text-gray-700 mb-2">Cantidad *</label><input id="consumo-cantidad" type="number" min="1" step="1" value={formData.cantidad} onChange={(e) => setFormData({ ...formData, cantidad: Number(e.target.value) })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required /></div><div><label htmlFor="consumo-precio" className="block text-sm font-medium text-gray-700 mb-2">Precio Unitario *</label><input id="consumo-precio" type="number" min="0.01" step="0.01" value={formData.precioUnitario} onChange={(e) => setFormData({ ...formData, precioUnitario: Number(e.target.value) })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required /></div></div><div className="flex justify-end gap-3"><Button type="button" variant="secondary" onClick={() => setIsModalOpen(false)}>Cancelar</Button><Button type="submit" variant="primary" disabled={enviando}>{enviando ? "Registrando..." : "Agregar"}</Button></div></form></Modal>
    </div>;
}