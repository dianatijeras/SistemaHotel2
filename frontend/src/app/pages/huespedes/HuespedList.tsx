import { useEffect, useState } from "react";
import { Plus, Edit, Trash2 } from "lucide-react";
import { Table } from "../../components/ui/Table";
import { Button } from "../../components/ui/Button";
import { Modal } from "../../components/ui/Modal";
import { Huesped } from "../../types";
import { hotelService } from "../../services/hotelService";

/**
 * Componente para listar y gestionar huéspedes del hotel. Permite crear, editar y eliminar huéspedes mediante un modal.
 * @constructor
 */
export function HuespedList() {
    const [huespedes, setHuespedes] = useState<Huesped[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingHuesped, setEditingHuesped] = useState<Huesped | null>(null);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(true);
    const [formData, setFormData] = useState({
        nombre: "",
        apellido: "",
        documento: "",
        email: "",
        telefono: "",
    });

    const cargarHuespedes = async () => {
        try {
            setLoading(true);
            setError("");
            setHuespedes(await hotelService.listarHuespedes());
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error cargando huéspedes");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        cargarHuespedes();
    }, []);

    const columns = [
        { key: "id", label: "ID" },
        { key: "documento", label: "Documento" },
        { key: "nombre", label: "Nombre" },
        { key: "apellido", label: "Apellido" },
        { key: "email", label: "Email" },
        { key: "telefono", label: "Teléfono" },
    ];

    const handleOpenModal = (huesped?: Huesped) => {
        if (huesped) {
            setEditingHuesped(huesped);
            setFormData({
                nombre: huesped.nombre,
                apellido: huesped.apellido,
                documento: huesped.documento ?? "",
                email: huesped.email,
                telefono: huesped.telefono,
            });
        } else {
            setEditingHuesped(null);
            setFormData({ nombre: "", apellido: "", documento: "", email: "", telefono: "" });
        }
        setIsModalOpen(true);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            setError("");
            if (editingHuesped) {
                await hotelService.actualizarHuesped(editingHuesped.id, {
                    nombre: formData.nombre,
                    apellido: formData.apellido,
                    documento: formData.documento,
                    telefono: formData.telefono,
                    correo: formData.email,
                });
            } else {
                await hotelService.crearHuesped({
                    nombre: formData.nombre,
                    apellido: formData.apellido,
                    documento: formData.documento,
                    telefono: formData.telefono,
                    correo: formData.email,
                });
            }
            setIsModalOpen(false);
            await cargarHuespedes();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error guardando huésped");
        }
    };

    const handleDelete = async (id: string) => {
        if (!confirm("¿Está seguro de eliminar este huésped?")) return;
        try {
            await hotelService.eliminarHuesped(id);
            await cargarHuespedes();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error eliminando huésped");
        }
    };

    return (
        <div className="p-8">
            <div className="flex items-center justify-between mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-gray-900">Huéspedes</h1>
                    <p className="text-gray-500 mt-1">Gestión de huéspedes del hotel</p>
                </div>
                <Button variant="primary" size="lg" onClick={() => handleOpenModal()}>
                    <div className="flex items-center gap-2"><Plus className="w-5 h-5" />Nuevo Huésped</div>
                </Button>
            </div>

            {error && <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">{error}</div>}
            {loading ? <p className="text-gray-500">Cargando huéspedes...</p> : (
                <Table
                    data={huespedes}
                    columns={columns}
                    searchPlaceholder="Buscar por nombre o email..."
                    actions={(huesped) => (
                        <>
                            <Button variant="secondary" size="sm" onClick={() => handleOpenModal(huesped)}><Edit className="w-4 h-4" /></Button>
                            <Button variant="danger" size="sm" onClick={() => handleDelete(huesped.id)}><Trash2 className="w-4 h-4" /></Button>
                        </>
                    )}
                />
            )}

            <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title={editingHuesped ? "Editar Huésped" : "Nuevo Huésped"}>
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                        <div><label htmlFor="huesped-nombre" className="block text-sm font-medium text-gray-700 mb-2">Nombre *</label><input id="huesped-nombre" value={formData.nombre} onChange={(e) => setFormData({ ...formData, nombre: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required /></div>
                        <div><label htmlFor="huesped-apellido" className="block text-sm font-medium text-gray-700 mb-2">Apellido *</label><input id="huesped-apellido" value={formData.apellido} onChange={(e) => setFormData({ ...formData, apellido: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required /></div>
                    </div>
                    <div><label htmlFor="huesped-documento" className="block text-sm font-medium text-gray-700 mb-2">Documento de identidad *</label><input id="huesped-documento" value={formData.documento} onChange={(e) => setFormData({ ...formData, documento: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" placeholder="Ej: CC10000004" required /><p className="text-xs text-gray-500 mt-1">Debe ser único en el hotel.</p></div>
                    <div><label htmlFor="huesped-email" className="block text-sm font-medium text-gray-700 mb-2">Email *</label><input id="huesped-email" type="email" value={formData.email} onChange={(e) => setFormData({ ...formData, email: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required /></div>
                    <div><label htmlFor="huesped-telefono" className="block text-sm font-medium text-gray-700 mb-2">Teléfono *</label><input id="huesped-telefono" value={formData.telefono} onChange={(e) => setFormData({ ...formData, telefono: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required /></div>
                    <div className="flex justify-end gap-3 pt-4">
                        <Button type="button" variant="secondary" onClick={() => setIsModalOpen(false)}>Cancelar</Button>
                        <Button type="submit" variant="primary">Guardar</Button>
                    </div>
                </form>
            </Modal>
        </div>
    );
}
