import { Outlet, Link, useLocation, useNavigate } from "react-router";
import {
    LayoutDashboard,
    Calendar,
    Users,
    DoorOpen,
    LogIn,
    LogOut,
    CreditCard,
    Hotel,
    UserCog,
    ChevronDown
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import { RolUsuario } from "../../types/auth";
import { useState, useEffect, useRef } from "react";

export function MainLayout() {
    const location = useLocation();
    const navigate = useNavigate();
    const { usuario, logout } = useAuth();
    const [showUserMenu, setShowUserMenu] = useState(false);
    const menuRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
                setShowUserMenu(false);
            }
        };

        if (showUserMenu) {
            document.addEventListener("mousedown", handleClickOutside);
            return () => document.removeEventListener("mousedown", handleClickOutside);
        }
    }, [showUserMenu]);

    // Navegación dinámica según rol
    const getNavItems = () => {
        const adminItems = [
            { path: "/", label: "Dashboard", icon: LayoutDashboard },
            { path: "/usuarios", label: "Personal del hotel", icon: UserCog },
            { path: "/habitaciones", label: "Habitaciones", icon: DoorOpen },
            { path: "/pagos", label: "Pagos", icon: CreditCard },
        ];

        const recepcionistaItems = [
            { path: "/", label: "Dashboard", icon: LayoutDashboard },
            { path: "/huespedes", label: "Huéspedes", icon: Users },
            { path: "/reservas", label: "Reservas", icon: Calendar },
            { path: "/check-in", label: "Check-in", icon: LogIn },
            { path: "/check-out", label: "Check-out", icon: LogOut },
            { path: "/habitaciones", label: "Habitaciones", icon: DoorOpen },
            { path: "/pagos", label: "Pagos", icon: CreditCard },
        ];

        return usuario?.rol === RolUsuario.ADMIN ? adminItems : recepcionistaItems;
    };

    const navItems = getNavItems();

    const isActive = (path: string) => {
        if (path === "/") return location.pathname === "/";
        return location.pathname.startsWith(path);
    };

    const handleLogout = () => {
        logout();
        navigate("/login");
    };

    const getRolColor = (rol: RolUsuario) => {
        if (rol === RolUsuario.ADMIN) return "bg-purple-600";
        if (rol === RolUsuario.PERSONAL_LIMPIEZA) return "bg-green-600";
        return "bg-blue-600";
    };

    const getRolLabel = (rol: RolUsuario) => {
        if (rol === RolUsuario.ADMIN) return "Administrador";
        if (rol === RolUsuario.PERSONAL_LIMPIEZA) return "Personal de limpieza";
        return "Recepcionista";
    };

    return (
        <div className="flex h-screen bg-gray-50">
            {/* Sidebar */}
            <aside className="w-64 bg-white border-r border-gray-200 flex flex-col">
                <div className="p-6 border-b border-gray-200">
                    <div className="flex items-center gap-3">
                        <Hotel className="w-8 h-8 text-blue-600" />
                        <div>
                            <h1 className="font-semibold text-gray-900">HotelManager</h1>
                            <p className="text-xs text-gray-500">Sistema de Gestión</p>
                        </div>
                    </div>
                </div>

                <nav className="flex-1 p-4 space-y-1 overflow-y-auto">
                    {navItems.map((item) => {
                        const Icon = item.icon;
                        const active = isActive(item.path);

                        return (
                            <Link
                                key={item.path}
                                to={item.path}
                                className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                                    active
                                        ? "bg-blue-50 text-blue-700"
                                        : "text-gray-700 hover:bg-gray-50"
                                }`}
                            >
                                <Icon className="w-5 h-5" />
                                <span className="font-medium">{item.label}</span>
                            </Link>
                        );
                    })}
                </nav>

                {/* User Info */}
                <div className="p-4 border-t border-gray-200">
                    <div className="relative" ref={menuRef}>
                        <button
                            onClick={() => setShowUserMenu(!showUserMenu)}
                            className="w-full flex items-center gap-3 px-4 py-3 rounded-lg hover:bg-gray-50 transition-colors"
                        >
                            <div
                                className={`w-10 h-10 rounded-full ${getRolColor(
                                    usuario?.rol!
                                )} flex items-center justify-center text-white font-semibold`}
                            >
                                {usuario?.nombre.charAt(0)}
                            </div>
                            <div className="flex-1 text-left">
                                <p className="font-medium text-gray-900 text-sm">
                                    {usuario?.nombre} {usuario?.apellido}
                                </p>
                                <p className="text-xs text-gray-500">{getRolLabel(usuario?.rol!)}</p>
                            </div>
                            <ChevronDown className="w-4 h-4 text-gray-400" />
                        </button>

                        {showUserMenu && (
                            <div className="absolute bottom-full left-0 right-0 mb-2 bg-white border border-gray-200 rounded-lg shadow-lg overflow-hidden">
                                <div className="p-3 border-b border-gray-200 bg-gray-50">
                                    <p className="text-xs text-gray-600">Sesión iniciada como</p>
                                    <p className="text-sm font-medium text-gray-900">@{usuario?.username}</p>
                                </div>
                                <button
                                    onClick={handleLogout}
                                    className="w-full px-4 py-3 text-left text-sm text-red-600 hover:bg-red-50 transition-colors flex items-center gap-2"
                                >
                                    <LogOut className="w-4 h-4" />
                                    Cerrar Sesión
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </aside>

            {/* Main Content */}
            <main className="flex-1 overflow-auto">
                <Outlet />
            </main>
        </div>
    );
}
