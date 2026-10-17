import { useEffect, useState } from "react";
import { Link } from "react-router";
import { DoorOpen, Users, Calendar, CreditCard, Plus, LogIn, LogOut, UserCog } from "lucide-react";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { useAuth } from "../context/AuthContext";
import { RolUsuario } from "../types/auth";
import { DashboardStats, Reserva } from "../types";
import { hotelService } from "../services/hotelService";

/**
 * Dashboard principal del sistema, muestra estadísticas clave y accesos rápidos según el rol del usuario.
 */
const statsIniciales: DashboardStats = {
    totalHabitaciones: 0,
    habitacionesDisponibles: 0,
    habitacionesOcupadas: 0,
    habitacionesEnLimpieza: 0,
    totalHuespedes: 0,
    totalReservas: 0,
    reservasActivas: 0,
    reservasEnProceso: 0,
    estadiasEnCurso: 0,
    pagosPendientes: 0,
    totalPagado: 0,
    checkoutsHoy: 0,
    totalUsuarios: 0,
    totalRecepcionistas: 0,
    totalPersonalLimpieza: 0,
};


const formatoMoneda = (valor: number) =>
    new Intl.NumberFormat("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 }).format(valor);

export function Dashboard() {
    const { usuario } = useAuth();
    const [stats, setStats] = useState<DashboardStats>(statsIniciales);
    const [reservasRecientes, setReservasRecientes] = useState<Reserva[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const cargarDashboard = async () => {
            try {
                setLoading(true);
                setError("");
                const [statsData, reservasData] = await Promise.all([
                    hotelService.obtenerDashboardStats(),
                    hotelService.listarReservas(),
                ]);
                setStats(statsData);
                setReservasRecientes(reservasData.slice(-3).reverse());
            } catch (err) {
                setError(err instanceof Error ? err.message : "Error cargando dashboard");
            } finally {
                setLoading(false);
            }
        };

        cargarDashboard();
    }, []);

    const statsCards = usuario?.rol === RolUsuario.ADMIN
        ? [
            {
                label: "Personal del Hotel",
                value: String(stats.totalUsuarios),
                detail: `${stats.totalRecepcionistas} recep. / ${stats.totalPersonalLimpieza} limpieza`,
                icon: UserCog,
                color: "text-purple-600",
                bg: "bg-purple-50",
            },
            {
                label: "Habitaciones Disponibles",
                value: String(stats.habitacionesDisponibles),
                total: String(stats.totalHabitaciones),
                icon: DoorOpen,
                color: "text-green-600",
                bg: "bg-green-50",
            },
            {
                label: "Reservas Activas",
                value: String(stats.reservasActivas),
                detail: `${stats.totalReservas} reservas totales`,
                icon: Calendar,
                color: "text-blue-600",
                bg: "bg-blue-50",
            },
            {
                label: "Total Pagado",
                value: formatoMoneda(stats.totalPagado),
                detail: `${stats.pagosPendientes} pagos pendientes`,
                icon: CreditCard,
                color: "text-yellow-600",
                bg: "bg-yellow-50",
            },
        ]
        : [
            {
                label: "Huéspedes Registrados",
                value: String(stats.totalHuespedes),
                icon: Users,
                color: "text-blue-600",
                bg: "bg-blue-50",
            },
            {
                label: "Reservas en Proceso",
                value: String(stats.reservasEnProceso),
                detail: "Reservadas o confirmadas",
                icon: Calendar,
                color: "text-purple-600",
                bg: "bg-purple-50",
            },
            {
                label: "Estadías en Curso",
                value: String(stats.estadiasEnCurso),
                icon: LogIn,
                color: "text-green-600",
                bg: "bg-green-50",
            },
            {
                label: "Pagos Pendientes",
                value: String(stats.pagosPendientes),
                detail: `${stats.checkoutsHoy} check-outs hoy`,
                icon: CreditCard,
                color: "text-yellow-600",
                bg: "bg-yellow-50",
            },
        ];

    const quickActions = usuario?.rol === RolUsuario.ADMIN
        ? [
            { label: "Gestionar Personal", icon: UserCog, to: "/usuarios", variant: "primary" as const },
            { label: "Ver Habitaciones", icon: DoorOpen, to: "/habitaciones", variant: "secondary" as const },
            { label: "Ver Pagos", icon: CreditCard, to: "/pagos", variant: "secondary" as const },
        ]
        : [
            { label: "Nuevo Huésped", icon: Users, to: "/huespedes", variant: "primary" as const },
            { label: "Nueva Reserva", icon: Plus, to: "/reservas/nueva", variant: "secondary" as const },
            { label: "Check-in", icon: LogIn, to: "/check-in", variant: "success" as const },
            { label: "Check-out", icon: LogOut, to: "/check-out", variant: "secondary" as const },
        ];

    return (
        <div className="p-8">
            <div className="mb-8">
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
                        <p className="text-gray-500 mt-1">
                            {usuario?.rol === RolUsuario.ADMIN
                                ? "Panel de administración del sistema"
                                : "Resumen operativo del día"}
                        </p>
                    </div>
                    <div className="text-right">
                        <p className="text-sm text-gray-600">Bienvenido/a</p>
                        <p className="text-lg font-semibold text-gray-900">
                            {usuario?.nombre} {usuario?.apellido}
                        </p>
                        <span className={`inline-block px-2 py-1 text-xs font-medium rounded-full ${usuario?.rol === RolUsuario.ADMIN ? "bg-purple-100 text-purple-800" : "bg-blue-100 text-blue-800"}`}>
              {usuario?.rol}
            </span>
                    </div>
                </div>
            </div>

            {error && <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">{error}</div>}
            {loading && <p className="mb-4 text-gray-500">Cargando datos reales del sistema...</p>}

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                {statsCards.map((stat) => {
                    const Icon = stat.icon;
                    return (
                        <Card key={stat.label}>
                            <div className="flex items-start justify-between">
                                <div>
                                    <p className="text-sm text-gray-600 mb-1">{stat.label}</p>
                                    <div className="flex items-baseline gap-2">
                                        <p className="text-3xl font-bold text-gray-900">{stat.value}</p>
                                        {stat.total && <p className="text-sm text-gray-500">/ {stat.total}</p>}
                                    </div>
                                    {stat.detail && <p className="text-xs text-gray-500 mt-1">{stat.detail}</p>}
                                </div>
                                <div className={`${stat.bg} ${stat.color} p-3 rounded-lg`}>
                                    <Icon className="w-6 h-6" />
                                </div>
                            </div>
                        </Card>
                    );
                })}
            </div>

            <Card className="mb-8">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Acciones Rápidas</h2>
                <div className="flex gap-4 flex-wrap">
                    {quickActions.map((action) => {
                        const Icon = action.icon;
                        return (
                            <Link key={action.to} to={action.to}>
                                <Button variant={action.variant} size="lg">
                                    <div className="flex items-center gap-2">
                                        <Icon className="w-5 h-5" />
                                        {action.label}
                                    </div>
                                </Button>
                            </Link>
                        );
                    })}
                </div>
            </Card>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                    <h2 className="text-lg font-semibold text-gray-900 mb-4">Reservas Recientes</h2>
                    <div className="space-y-3">
                        {reservasRecientes.length === 0 ? <p className="text-sm text-gray-500">No hay reservas registradas.</p> : reservasRecientes.map((reserva) => (
                            <div key={reserva.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                                <div>
                                    <p className="font-medium text-gray-900">{reserva.huesped?.nombre} {reserva.huesped?.apellido}</p>
                                    <p className="text-sm text-gray-500">Hab. {reserva.numeroHabitacion} - {reserva.fechaInicio} al {reserva.fechaFin}</p>
                                </div>
                                <span className="px-2 py-1 text-xs font-medium rounded-full bg-green-100 text-green-800">
                  {reserva.estado}
                </span>
                            </div>
                        ))}
                    </div>
                </Card>

                <Card>
                    <h2 className="text-lg font-semibold text-gray-900 mb-4">Resumen Real</h2>
                    <div className="space-y-3 text-sm">
                        <div className="flex justify-between p-3 bg-gray-50 rounded-lg"><span>Habitaciones ocupadas</span><strong>{stats.habitacionesOcupadas}</strong></div>
                        <div className="flex justify-between p-3 bg-gray-50 rounded-lg"><span>Habitaciones en limpieza</span><strong>{stats.habitacionesEnLimpieza}</strong></div>
                        <div className="flex justify-between p-3 bg-gray-50 rounded-lg"><span>Estadías activas</span><strong>{stats.estadiasEnCurso}</strong></div>
                        <div className="flex justify-between p-3 bg-gray-50 rounded-lg"><span>Pagos pendientes</span><strong>{stats.pagosPendientes}</strong></div>
                    </div>
                </Card>
            </div>
        </div>
    );
}
