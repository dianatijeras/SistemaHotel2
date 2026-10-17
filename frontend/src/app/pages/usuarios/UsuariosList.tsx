import { useEffect, useState } from "react";
import { Plus, Power, PowerOff } from "lucide-react";
import { Table } from "../../components/ui/Table";
import { Button } from "../../components/ui/Button";
import { Modal } from "../../components/ui/Modal";
import { RolUsuario, TurnoRecepcionista, Usuario } from "../../types/auth";
import { hotelService } from "../../services/hotelService";

type RolPersonalFormulario = RolUsuario.RECEPCIONISTA | RolUsuario.PERSONAL_LIMPIEZA;

interface PersonalFormData {
    rol: RolPersonalFormulario;
    nombre: string;
    apellido: string;
    turno: TurnoRecepcionista;
    username: string;
    password: string;
}

const formInicial: PersonalFormData = {
    rol: RolUsuario.RECEPCIONISTA,
    nombre: "",
    apellido: "",
    turno: TurnoRecepcionista.MANANA,
    username: "",
    password: "",
};

const colorRol = (rol: RolUsuario) => {
    if (rol === RolUsuario.ADMIN) return "bg-purple-100 text-purple-800";
    if (rol === RolUsuario.PERSONAL_LIMPIEZA) return "bg-green-100 text-green-800";
    return "bg-blue-100 text-blue-800";
};

export function UsuariosList() {
    const [usuarios, setUsuarios] = useState<Usuario[]>([]);
    const [error, setError] = useState("");
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [guardando, setGuardando] = useState(false);
    const [formData, setFormData] = useState<PersonalFormData>(formInicial);

    const cargar = async () => {
        try {
            setError("");
            setUsuarios(await hotelService.listarUsuarios());
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error cargando usuarios");
        }
    };

    useEffect(() => { cargar(); }, []);

    const abrirModal = () => {
        setFormData(formInicial);
        setError("");
        setIsModalOpen(true);
    };

    const crearPersonal = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!formData.nombre.trim() || !formData.apellido.trim() || (formData.rol === RolUsuario.RECEPCIONISTA && (!formData.username.trim() || !formData.password.trim()))) {
            setError("Todos los campos del personal son obligatorios.");
            return;
        }

        if (formData.rol === RolUsuario.RECEPCIONISTA && formData.password.length < 4) {
            setError("La contraseña debe tener mínimo 4 caracteres.");
            return;
        }

        try {
            setGuardando(true);
            setError("");

            if (formData.rol === RolUsuario.RECEPCIONISTA) {
                await hotelService.crearRecepcionista({
                    nombre: formData.nombre.trim(),
                    apellido: formData.apellido.trim(),
                    turno: formData.turno,
                    username: formData.username.trim(),
                    password: formData.password,
                });
            } else {
                await hotelService.crearPersonalLimpieza({
                    nombre: formData.nombre.trim(),
                    apellido: formData.apellido.trim(),
                });
            }

            setIsModalOpen(false);
            await cargar();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error creando personal del hotel");
        } finally {
            setGuardando(false);
        }
    };

    const columns = [
        { key: "id", label: "ID" },
        { key: "username", label: "Usuario" },
        { key: "nombreCompleto", label: "Nombre Completo", render: (u: Usuario) => `${u.nombre} ${u.apellido}` },
        { key: "rol", label: "Rol", render: (u: Usuario) => <span className={`px-2 py-1 text-xs font-medium rounded-full ${colorRol(u.rol)}`}>{u.rol}</span> },
        { key: "activo", label: "Estado", render: (u: Usuario) => <span className={`px-2 py-1 text-xs font-medium rounded-full ${u.activo ? "bg-green-100 text-green-800" : "bg-gray-100 text-gray-800"}`}>{u.activo ? "ACTIVO" : "INACTIVO"}</span> },
    ];

    const toggleActivo = async (id: string) => {
        try {
            setError("");
            await hotelService.cambiarEstadoUsuario(id);
            await cargar();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error cambiando estado");
        }
    };

    return <div className="p-8">
        <div className="flex items-center justify-between mb-8">
            <div>
                <h1 className="text-3xl font-bold text-gray-900">Gestión de Personal</h1>
                <p className="text-gray-500 mt-1">Registrar recepcionistas y personal de limpieza del hotel</p>
            </div>
            <Button variant="primary" size="lg" onClick={abrirModal}>
                <div className="flex items-center gap-2"><Plus className="w-5 h-5" />Nuevo personal</div>
            </Button>
        </div>

        {error && <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">{error}</div>}

        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
            <div className="bg-white border border-gray-200 rounded-lg p-6"><p className="text-sm text-gray-600">Total Personal</p><p className="text-3xl font-bold text-gray-900">{usuarios.length}</p></div>
            <div className="bg-white border border-gray-200 rounded-lg p-6"><p className="text-sm text-gray-600">Usuarios Activos</p><p className="text-3xl font-bold text-green-600">{usuarios.filter((u) => u.activo).length}</p></div>
            <div className="bg-white border border-gray-200 rounded-lg p-6"><p className="text-sm text-gray-600">Recepcionistas</p><p className="text-3xl font-bold text-blue-600">{usuarios.filter((u) => u.rol === RolUsuario.RECEPCIONISTA).length}</p></div>
            <div className="bg-white border border-gray-200 rounded-lg p-6"><p className="text-sm text-gray-600">Limpieza</p><p className="text-3xl font-bold text-green-600">{usuarios.filter((u) => u.rol === RolUsuario.PERSONAL_LIMPIEZA).length}</p></div>
        </div>

        <Table data={usuarios} columns={columns} searchPlaceholder="Buscar por usuario, nombre..." actions={(usuario) => <Button variant={usuario.activo ? "danger" : "success"} size="sm" onClick={() => toggleActivo(usuario.id)}>{usuario.activo ? <PowerOff className="w-4 h-4" /> : <Power className="w-4 h-4" />}</Button>} />

        <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title="Crear personal del hotel" size="md">
            <form onSubmit={crearPersonal} className="space-y-5">
                <div>
                    <label htmlFor="personal-rol" className="block text-sm font-medium text-gray-700 mb-2">Tipo de personal *</label>
                    <select id="personal-rol" value={formData.rol} onChange={(e) => setFormData({ ...formData, rol: e.target.value as RolPersonalFormulario })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required>
                        <option value={RolUsuario.RECEPCIONISTA}>Recepcionista</option>
                        <option value={RolUsuario.PERSONAL_LIMPIEZA}>Personal de limpieza</option>
                    </select>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                        <label htmlFor="personal-nombre" className="block text-sm font-medium text-gray-700 mb-2">Nombre *</label>
                        <input id="personal-nombre" value={formData.nombre} onChange={(e) => setFormData({ ...formData, nombre: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required />
                    </div>
                    <div>
                        <label htmlFor="personal-apellido" className="block text-sm font-medium text-gray-700 mb-2">Apellido *</label>
                        <input id="personal-apellido" value={formData.apellido} onChange={(e) => setFormData({ ...formData, apellido: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required />
                    </div>
                </div>

                {formData.rol === RolUsuario.RECEPCIONISTA && <div>
                    <label htmlFor="personal-turno" className="block text-sm font-medium text-gray-700 mb-2">Turno *</label>
                    <select id="personal-turno" value={formData.turno} onChange={(e) => setFormData({ ...formData, turno: e.target.value as TurnoRecepcionista })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required>
                        <option value={TurnoRecepcionista.MANANA}>Mañana</option>
                        <option value={TurnoRecepcionista.TARDE}>Tarde</option>
                        <option value={TurnoRecepcionista.NOCHE}>Noche</option>
                    </select>
                </div>}

                {formData.rol === RolUsuario.RECEPCIONISTA && <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                        <label htmlFor="personal-usuario" className="block text-sm font-medium text-gray-700 mb-2">Usuario *</label>
                        <input id="personal-usuario" value={formData.username} onChange={(e) => setFormData({ ...formData, username: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required />
                    </div>
                    <div>
                        <label htmlFor="personal-contrasena" className="block text-sm font-medium text-gray-700 mb-2">Contraseña *</label>
                        <input id="personal-contrasena" type="password" value={formData.password} onChange={(e) => setFormData({ ...formData, password: e.target.value })} className="w-full px-4 py-2 border border-gray-300 rounded-lg" required minLength={4} />
                    </div>
                </div>}

                <div className="flex justify-end gap-3">
                    <Button type="button" variant="secondary" onClick={() => setIsModalOpen(false)}>Cancelar</Button>
                    <Button type="submit" variant="primary" disabled={guardando}>{guardando ? "Guardando..." : "Crear personal"}</Button>
                </div>
            </form>
        </Modal>
    </div>;
}
